package com.stats.service;

import com.analysis.constant.EventType;
import com.analysis.entity.Analysis;
import com.stats.dto.MonitorStatsDto;
import com.stats.testsupport.AnalysisDataSeeder;
import com.stats.testsupport.AnalysisFixtureFactory;
import com.stats.testsupport.AnalysisFixtureFactory.DaySpec;
import com.stats.testsupport.AnalysisFixtureFactory.SessionDraft;
import com.stats.testsupport.HibernateQueryCounter;
import com.stats.testsupport.HibernateQueryCounter.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.stats.testsupport.AnalysisFixtureFactory.EventDraft;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿼리 횟수 / 엔티티 로딩 수 테스트.
 *
 * <p><b>지금은 일부가 빨간불이다. 의도된 실패다.</b> 리팩터링 후 초록불이 되는 것이
 * 개선의 증거가 된다. 실패하는 테스트를 먼저 커밋해 두면 "무엇을 고쳤는지"가
 * 커밋 히스토리에 남는다.
 *
 * <p>응답 시간은 assert 하지 않는다. 참고용으로 로그에만 남긴다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({AnalysisDataSeeder.class, HibernateQueryCounter.class})
class MonitorStatsQueryCountTest {

    private static final AtomicLong MEMBER_ID_SEQ = new AtomicLong(950_000);

    @Autowired
    private MonitorStatsService monitorStatsService;

    @Autowired
    private AnalysisDataSeeder seeder;

    @Autowired
    private HibernateQueryCounter counter;

    private long memberId;

    @BeforeEach
    void setUp() {
        memberId = MEMBER_ID_SEQ.incrementAndGet();
    }

    @Test
    @DisplayName("[초록] day 통계의 쿼리 수는 세션 수와 무관하게 일정하다")
    void dayQueryCountIsIndependentOfSessionCount() {
        long lightMemberId = MEMBER_ID_SEQ.incrementAndGet();
        seedDay(lightMemberId, new DaySpec(3, 10, 60, 120, 20, 60, 9));
        seedDay(memberId, new DaySpec(20, 10, 60, 120, 20, 60, 9));

        long lightQueries = counter
                .measure(() -> monitorStatsService.getMonitorStats("day", lightMemberId))
                .measurement()
                .queryCount();

        long heavyQueries = counter
                .measure(() -> monitorStatsService.getMonitorStats("day", memberId))
                .measurement()
                .queryCount();

        // 엔티티가 원시 Long ID 를 들고 있고 findByAnalysisIdIn 으로 배치 조회하므로
        // 세션이 늘어도 쿼리 수는 그대로여야 한다. 이 성질이 깨지면 N+1 이 생긴 것이다.
        assertThat(heavyQueries).isEqualTo(lightQueries);
        assertThat(heavyQueries).isEqualTo(2);
    }

    @Test
    @DisplayName("[빨강] day 통계가 기간 밖 이벤트까지 로딩한다")
    void dayShouldNotLoadEventsOutsidePeriod() {
        LocalDateTime midnight = LocalDate.now().atStartOfDay();

        // 어제 저녁에 시작해 오늘 새벽까지 이어진 세션.
        // 어제 이벤트 200건, 오늘 이벤트 4건.
        List<EventDraft> events = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            events.add(new EventDraft(EventType.BAD_POSTURE, midnight.minusHours(6).plusSeconds(i * 100L), 55.0));
            events.add(new EventDraft(EventType.GOOD_POSTURE, midnight.minusHours(6).plusSeconds(i * 100L + 50), 14.0));
        }
        events.add(new EventDraft(EventType.BAD_POSTURE, midnight.plusMinutes(5), 55.0));
        events.add(new EventDraft(EventType.GOOD_POSTURE, midnight.plusMinutes(10), 14.0));

        Analysis analysis = new Analysis();
        analysis.setMemberId(memberId);
        analysis.setStartedAt(midnight.minusHours(6));
        analysis.setEndedAt(midnight.plusMinutes(30));

        seeder.seed(List.of(new SessionDraft(analysis, events)));

        Result<MonitorStatsDto> result =
                counter.measure(() -> monitorStatsService.getMonitorStats("day", memberId));

        System.out.println("[day / straddling] " + result.measurement());

        // 오늘 필요한 건 세션 1건 + 이벤트 2건.
        // findByAnalysisIdInAndEventAtBetween 을 쓰면 어제 200건은 안 와야 한다.
        assertThat(result.measurement().entityLoadCount()).isLessThan(20);
    }

    /**
     * 이 테스트가 문제 2의 핵심 증거다.
     *
     * <p>30일치 이벤트 약 7,200건을 전부 메모리에 올리지만, 실제 소비처는
     * createLogs(최근 6건)와 calculateHourlyPattern(10칸)뿐이다.
     * 개선안: findTop20ByAnalysisIdInOrderByEventAtDesc + GROUP BY HOUR() 네이티브 쿼리.
     */
    @Test
    @DisplayName("[빨강] month 통계가 7,200건을 올려서 6건만 쓰고 버린다")
    void monthShouldNotLoadEntireEventHistory() {
        List<SessionDraft> drafts =
                AnalysisFixtureFactory.flatMonth(memberId, LocalDate.now(), 42L);
        AnalysisDataSeeder.SeedResult seeded = seeder.seedWithDailyStats(drafts);

        System.out.println("[seed] 세션 " + seeded.sessionCount() + " / 이벤트 " + seeded.eventCount());

        Result<MonitorStatsDto> result =
                counter.measure(() -> monitorStatsService.getMonitorStats("month", memberId));

        System.out.println("[month] " + result.measurement());

        // 개선 후 실측 140건 (daily_stats 30 + Analysis 90 + 로그 20).
        // 데이터가 늘어도 이 값은 고정이어야 한다. 200을 넘으면 전량 로딩이 되살아난 것.
        assertThat(result.measurement().entityLoadCount()).isLessThan(200);
    }

    private void seedDay(long targetMemberId, DaySpec spec) {
        List<SessionDraft> drafts = new ArrayList<>(
                AnalysisFixtureFactory.buildDay(targetMemberId, LocalDate.now(), spec, 42L)
        );
        seeder.seed(drafts);
    }
}
