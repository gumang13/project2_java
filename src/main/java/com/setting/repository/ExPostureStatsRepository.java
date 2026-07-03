package com.setting.repository;

import com.stats.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 외부 자세 통계 API 전용 조회 리포지토리.
 * DailyStats 엔티티를 읽기 전용으로 조회한다. (stats 도메인의 DailyStatsRepository와 분리 — 소유권 경계 유지)
 */
public interface ExPostureStatsRepository extends JpaRepository<DailyStats, Long> {

    // 기간 내 전체 회원의 일일 통계 (모집단 평균·백분위 집계용)
    List<DailyStats> findByStatDateBetween(LocalDateTime from, LocalDateTime to);

    // 기간 내 특정 회원(키 소유자 본인)의 일일 통계
    List<DailyStats> findByMemberIdAndStatDateBetween(Long memberId, LocalDateTime from, LocalDateTime to);
}
