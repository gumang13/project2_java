package com.analysis.repository;

import com.analysis.entity.AnalysisEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface AnalysisEventRepository extends JpaRepository<AnalysisEvent, Long> {

    List<AnalysisEvent> findByAnalysisIdIn(List<Long> analysisId);

    List<AnalysisEvent> findByAnalysisIdInAndEventAtBetween(
            List<Long> analysisIds, LocalDateTime from, LocalDateTime to);

    //ByAnalysisId 필드 기준 전달 받은 id목록에 포함된 데이터 삭제후 반환없음
    //계정관리서비스 쪽 reset 관련
    void deleteByAnalysisIdIn(List<Long> analysisIds);
    // 로그용: 기간 안 최근 이벤트만. 6건을 쓰지만 여는 BAD 이벤트를 놓치지 않도록 여유를 둔다.
    List<AnalysisEvent> findTop20ByAnalysisIdInAndEventAtGreaterThanEqualAndEventAtLessThanOrderByEventAtDesc(
            List<Long> analysisIds, LocalDateTime from, LocalDateTime to);

    // 시간대 패턴용: 엔티티를 만들지 않고 DB에서 집계한다.
    // JdbcTemplate 이 아니라 리포지토리에 두어야 Hibernate Statistics 에 잡힌다.
    @Query(value = """
        SELECT HOUR(event_at) AS event_hour, COUNT(*) AS event_count
        FROM analysis_event
        WHERE analysis_id IN (:analysisIds)
          AND event_at >= :from
          AND event_at < :to
          AND event_type = 'BAD_POSTURE'
        GROUP BY HOUR(event_at)
        """, nativeQuery = true)
    List<Object[]> countBadPostureByHour(
            @Param("analysisIds") List<Long> analysisIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
