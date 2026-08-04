package com.stats.service;

import com.analysis.constant.EventType;
import com.analysis.entity.Analysis;
import com.stats.dto.MonitorStatsDto;
import com.stats.testsupport.AnalysisDataSeeder;
import com.stats.testsupport.AnalysisFixtureFactory.EventDraft;
import com.stats.testsupport.AnalysisFixtureFactory.SessionDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * day 경로 계산 로직 회귀 테스트.
 *
 * <p><b>목적:</b> 리팩터링 전에 현재 동작을 잠근다. 여기 있는 기대값은 코드 출력을
 * 베낀 게 아니라 손으로 계산한 값이다. 리팩터링 후에도 이 값들이 그대로여야 한다.
 *
 * <p><b>왜 public API 로 테스트하나:</b> calculateBadPostureStats 가 private 이라
 * 직접 못 부른다. 그리고 잠가야 할 것은 내부 구현이 아니라 프론트가 받는 DTO 값이다.
 * 이렇게 해두면 나중에 Calculator 클래스로 분리하든 groupingBy 로 바꾸든 안전하다.
 *
 * <p><b>시각 의존성:</b> MonitorStatsService 가 LocalDateTime.now() 를 직접 부르기
 * 때문에 to 값이 실행 시각에 따라 변한다. fixture 를 자정 직후 40분 안에 몰아넣고,
 * 그 시간대에 실행되면 스킵한다. 근본 해결은 Clock 주입이며 별도 작업으로 뺀다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({AnalysisDataSeeder.class})
class MonitorStatsDayRegressionTest {

    private static final AtomicLong MEMBER_ID_SEQ = new AtomicLong(900_000);

    @Autowired
    private MonitorStatsService monitorStatsService;

    @Autowired
    private AnalysisDataSeeder seeder;

    private long memberId;
    private LocalDateTime midnight;

    @BeforeEach
    void setUp() {
        assumeTrue(
                LocalTime.now().isAfter(LocalTime.of(1, 0)),
                "fixture 가 자정~00:40 구간을 쓰기 때문에 01:00 이전 실행은 건너뛴다"
        );

        memberId = MEMBER_ID_SEQ.incrementAndGet();
        midnight = LocalDate.now().atStartOfDay();
    }

    @Test
    @DisplayName("기간 시작 이전에 시작된 거북목은 기간 안쪽만 집계한다")
    void clipsBadPostureAtPeriodStart() {
        // 어제 23:40 ~ 오늘 00:20 세션, BAD 는 어제 23:50 에 시작해 오늘 00:10 에 해제
        seeder.seed(List.of(new SessionDraft(
                session(midnight.minusMinutes(20), midnight.plusMinutes(20)),
                List.of(
                        bad(midnight.minusMinutes(10)),
                        good(midnight.plusMinutes(10))
                )
        )));

        MonitorStatsDto stats = monitorStatsService.getMonitorStats("day", memberId);

        // 측정 시간: 00:00~00:20 = 20분 (00:00 이전은 오늘 기간이 아님)
        // 거북목:   00:00~00:10 = 10분 (23:50~00:00 구간은 잘려나감)
        assertThat(stats.getBadMinutes()).isEqualTo(10);
        assertThat(stats.getGoodMinutes()).isEqualTo(10);
        assertThat(stats.getRatio()).isEqualTo(50);

        // BAD 이벤트 발생 시각이 기간 밖이므로 알림 카운트에는 안 잡힌다
        assertThat(stats.getAlerts()).isZero();
    }

    @Test
    @DisplayName("해제되지 않은 거북목은 세션 종료 시각까지만 집계한다")
    void unclosedBadPostureStopsAtSessionEnd() {
        // 00:00 ~ 00:30 세션, 00:10 에 BAD 발생 후 GOOD 없이 세션 종료
        seeder.seed(List.of(new SessionDraft(
                session(midnight, midnight.plusMinutes(30)),
                List.of(bad(midnight.plusMinutes(10)))
        )));

        MonitorStatsDto stats = monitorStatsService.getMonitorStats("day", memberId);

        // 거북목: 00:10~00:30 = 20분. 세션이 끝났으므로 now 까지 늘어나면 안 된다.
        assertThat(stats.getBadMinutes()).isEqualTo(20);
        assertThat(stats.getGoodMinutes()).isEqualTo(10);
        assertThat(stats.getRatio()).isEqualTo(33);
        assertThat(stats.getAlerts()).isEqualTo(1);
    }

    @Test
    @DisplayName("시간이 겹치는 두 세션의 거북목 상태는 서로 섞이지 않는다")
    void keepsSessionsIsolatedWhenTheyOverlap() {
        // A 세션의 BAD 가 열려 있는 동안 B 세션의 GOOD 이 와도 A 가 닫히면 안 된다.
        // 중첩 루프를 groupingBy 로 바꿔도 이 성질이 유지되어야 한다.
        Analysis a = session(midnight, midnight.plusMinutes(40));
        Analysis b = session(midnight.plusMinutes(10), midnight.plusMinutes(40));

        seeder.seed(List.of(
                new SessionDraft(a, List.of(
                        bad(midnight.plusMinutes(5)),
                        good(midnight.plusMinutes(15))
                )),
                new SessionDraft(b, List.of(
                        bad(midnight.plusMinutes(20)),
                        good(midnight.plusMinutes(25))
                ))
        ));

        MonitorStatsDto stats = monitorStatsService.getMonitorStats("day", memberId);

        // A 거북목 10분 + B 거북목 5분 = 15분
        // 측정 시간 A 40분 + B 30분 = 70분  →  바른 자세 55분
        assertThat(stats.getBadMinutes()).isEqualTo(15);
        assertThat(stats.getGoodMinutes()).isEqualTo(55);
        assertThat(stats.getRatio()).isEqualTo(79);
        assertThat(stats.getAlerts()).isEqualTo(2);
    }

    @Test
    @DisplayName("이벤트가 없는 세션도 측정 시간에는 포함된다")
    void countsMeasureTimeForSessionWithoutEvents() {
        seeder.seed(List.of(new SessionDraft(
                session(midnight, midnight.plusMinutes(30)),
                List.of()
        )));

        MonitorStatsDto stats = monitorStatsService.getMonitorStats("day", memberId);

        assertThat(stats.getBadMinutes()).isZero();
        assertThat(stats.getGoodMinutes()).isEqualTo(30);
        assertThat(stats.getRatio()).isEqualTo(100);
        assertThat(stats.getAlerts()).isZero();
    }

    @Test
    @DisplayName("측정 기록이 없으면 차트가 깨지지 않도록 빈 응답을 준다")
    void returnsEmptyStatsWhenNoAnalysisExists() {
        MonitorStatsDto stats = monitorStatsService.getMonitorStats("day", memberId);

        assertThat(stats.getRatio()).isZero();
        assertThat(stats.getLabels()).isEmpty();
        assertThat(stats.getLogs()).isEmpty();
        assertThat(stats.getHourly()).isEmpty();
    }

    @Test
    @DisplayName("로그는 기간 안의 이벤트만, 최근 6건까지만 내려준다")
    void keepsOnlySixMostRecentLogs() {
        LocalDateTime start = midnight;
        List<EventDraft> events = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            events.add(bad(start.plusMinutes(i * 4L + 1)));
            events.add(good(start.plusMinutes(i * 4L + 3)));
        }

        seeder.seed(List.of(new SessionDraft(
                session(start, start.plusMinutes(30)),
                events
        )));

        MonitorStatsDto stats = monitorStatsService.getMonitorStats("day", memberId);

        // 이벤트 10건 중 마지막 6건
        assertThat(stats.getLogs()).hasSize(6);
        assertThat(stats.getLogs().get(5).getType()).isEqualTo("recover");
        assertThat(stats.getLogs().get(5).getDur()).isEqualTo("2분 0초");
    }

    // -----------------------------------------------------------------
    // fixture 헬퍼
    // -----------------------------------------------------------------

    private Analysis session(LocalDateTime startedAt, LocalDateTime endedAt) {
        Analysis analysis = new Analysis();
        analysis.setMemberId(memberId);
        analysis.setStartedAt(startedAt);
        analysis.setEndedAt(endedAt);
        return analysis;
    }

    private EventDraft bad(LocalDateTime at) {
        return new EventDraft(EventType.BAD_POSTURE, at, 55.0);
    }

    private EventDraft good(LocalDateTime at) {
        return new EventDraft(EventType.GOOD_POSTURE, at, 14.0);
    }
}
