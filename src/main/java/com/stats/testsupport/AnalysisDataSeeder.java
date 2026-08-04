package com.stats.testsupport;

import com.analysis.constant.EventType;
import com.analysis.entity.Analysis;
import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;
import com.analysis.repository.AnalysisRepository;
import com.stats.testsupport.AnalysisFixtureFactory.EventDraft;
import com.stats.testsupport.AnalysisFixtureFactory.SessionDraft;
import com.stats.entity.DailyStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link AnalysisFixtureFactory} 가 만든 draft 를 실제로 저장한다.
 *
 * <p>Analysis 를 먼저 saveAll 해서 id 를 확보한 뒤, 그 id 로 AnalysisEvent 를 만든다.
 * (엔티티가 연관관계 없이 원시 Long 을 들고 있어서 이 순서가 강제된다.)
 *
 * <p>배치 insert 가 실제로 동작하려면 프로퍼티가 필요하다:
 * <pre>
 * spring.jpa.properties.hibernate.jdbc.batch_size=100
 * spring.jpa.properties.hibernate.order_inserts=true
 * </pre>
 * 참고: 엔티티가 {@code GenerationType.AUTO} 라서 MySQL 에서 시퀀스 테이블 방식으로
 * 동작하고, 그 덕분에 배치가 살아 있다. IDENTITY 였다면 Hibernate 가 JDBC 배치를
 * 통째로 비활성화했을 것이다.
 */
@Component
@RequiredArgsConstructor
public class AnalysisDataSeeder {

    private static final int FLUSH_EVERY = 500;

    private final AnalysisRepository analysisRepository;
    private final AnalysisEventRepository analysisEventRepository;
    private final EntityManager entityManager;

    /** 심은 결과 요약. 테스트에서 "정말 이만큼 들어갔나"를 assert 하는 데 쓴다. */
    public record SeedResult(
            int sessionCount,
            int eventCount,
            LocalDateTime firstEventAt,
            LocalDateTime lastEventAt
    ) {
    }

    @Transactional
    public SeedResult seed(List<SessionDraft> drafts) {
        // 1) 세션 먼저 저장해서 id 확보
        List<Analysis> analyses = drafts.stream().map(SessionDraft::analysis).toList();
        List<Analysis> saved = analysisRepository.saveAll(analyses);
        entityManager.flush();

        // 2) 확보한 id 로 이벤트 생성
        List<AnalysisEvent> events = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            Long analysisId = saved.get(i).getId();

            for (EventDraft draft : drafts.get(i).events()) {
                AnalysisEvent event = new AnalysisEvent();
                event.setAnalysisId(analysisId);
                event.setEventType(draft.type());
                event.setEventAt(draft.at());
                event.setAngle(draft.angle());
                events.add(event);
            }
        }

        // 3) 청크 단위로 저장 + 영속성 컨텍스트 비우기
        //    한 번에 다 넣으면 1차 캐시가 수천 건을 들고 있게 되어 flush 가 급격히 느려진다.
        for (int start = 0; start < events.size(); start += FLUSH_EVERY) {
            int end = Math.min(start + FLUSH_EVERY, events.size());
            analysisEventRepository.saveAll(events.subList(start, end));
            entityManager.flush();
            entityManager.clear();
        }

        LocalDateTime first = events.stream()
                .map(AnalysisEvent::getEventAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        LocalDateTime last = events.stream()
                .map(AnalysisEvent::getEventAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new SeedResult(saved.size(), events.size(), first, last);
    }

    /**
     * 세션과 이벤트를 심고, 거기서 계산한 값으로 daily_stats 까지 채운다.
     *
     * <p>week / month 경로는 buildRangeStats 가 daily_stats 를 먼저 조회하고
     * 비어 있으면 createEmptyStats 로 빠져나간다. 즉 daily_stats 가 없으면
     * 이벤트를 아예 안 읽어서 "전량 로딩" 문제가 측정되지 않는다.
     *
     * <p>집계 규칙은 MonitorStatsService 와 같다: BAD 이벤트에서 시작해 다음 GOOD
     * 에서 닫고, 닫히지 않으면 세션 종료 시각까지. 서비스 코드를 호출하지 않고
     * 독립적으로 다시 구현한 것이라, 두 값이 어긋나면 둘 중 하나가 틀린 것이다.
     */
    @Transactional
    public SeedResult seedWithDailyStats(List<SessionDraft> drafts) {
        SeedResult result = seed(drafts);

        Map<LocalDate, long[]> perDay = new LinkedHashMap<>();
        long memberId = drafts.isEmpty() ? 0L : drafts.get(0).analysis().getMemberId();

        for (SessionDraft draft : drafts) {
            Analysis analysis = draft.analysis();
            LocalDateTime sessionStart = analysis.getStartedAt();
            LocalDateTime sessionEnd = analysis.getEndedAt() != null
                    ? analysis.getEndedAt()
                    : LocalDateTime.now();

            long badSeconds = 0;
            int alerts = 0;
            LocalDateTime badStartedAt = null;

            for (EventDraft event : draft.events()) {
                if (event.type() == EventType.BAD_POSTURE) {
                    alerts++;
                    if (badStartedAt == null) {
                        badStartedAt = event.at();
                    }
                } else if (badStartedAt != null) {
                    badSeconds += ChronoUnit.SECONDS.between(badStartedAt, event.at());
                    badStartedAt = null;
                }
            }
            if (badStartedAt != null) {
                badSeconds += ChronoUnit.SECONDS.between(badStartedAt, sessionEnd);
            }

            long totalSeconds = ChronoUnit.SECONDS.between(sessionStart, sessionEnd);

            long[] bucket = perDay.computeIfAbsent(sessionStart.toLocalDate(), key -> new long[3]);
            bucket[0] += alerts;
            bucket[1] += badSeconds;
            bucket[2] += totalSeconds;
        }

        for (Map.Entry<LocalDate, long[]> entry : perDay.entrySet()) {
            long[] bucket = entry.getValue();
            int alerts = (int) bucket[0];
            int badSeconds = (int) bucket[1];
            int totalSeconds = (int) bucket[2];

            DailyStats dailyStats = DailyStats.create(memberId, entry.getKey().atStartOfDay());
            dailyStats.addMeasuredSeconds(alerts, totalSeconds - badSeconds, badSeconds, totalSeconds);
            entityManager.persist(dailyStats);
        }

        entityManager.flush();
        entityManager.clear();

        return result;
    }

    /**
     * 측정 전 정리. 회귀 테스트는 @Transactional 롤백으로 격리되지만,
     * 개발 DB 에 직접 심어서 응답시간을 재는 경우에는 이게 필요하다.
     */
    @Transactional
    public void clear(Long memberId) {
        List<Long> analysisIds = analysisRepository.findIdsByMemberId(memberId);
        if (!analysisIds.isEmpty()) {
            analysisEventRepository.deleteByAnalysisIdIn(analysisIds);
        }
        analysisRepository.deleteByMemberId(memberId);
    }
}
