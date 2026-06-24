package com.analysis.repository;

import com.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    @Query("SELECT a.id FROM Analysis a WHERE a.memberId = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);


    //통계 계산 시 기간 경계에 걸친 분석 세션과 진행 중인 분석 세션을 누락하지 않기 위해, memberId와 기간 겹침 조건으로 Analysis를 조회하는 쿼리를 추가
    // startedAt~endedAt이 요청 기간과 겹치는 세션을 조회
    @Query("""
            SELECT a FROM Analysis a
            WHERE a.memberId = :memberId
              AND a.startedAt < :to
              AND (a.endedAt IS NULL OR a.endedAt >= :from)
            """)
    List<Analysis> findByMemberIdAndPeriodOverlap(
            @Param("memberId") Long memberId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
