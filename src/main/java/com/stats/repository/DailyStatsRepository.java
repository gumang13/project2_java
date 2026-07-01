package com.stats.repository;

import com.stats.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {
    List<DailyStats> findByMemberIdAndStatDateBetween(Long memberId, LocalDateTime from, LocalDateTime to);

    // 외부 통계 API: 기간 내 전체 회원의 일일 통계(모집단). 익명 집계에 사용.
    List<DailyStats> findByStatDateBetween(LocalDateTime from, LocalDateTime to);

    //memberId 로 조회된 DailyStats 데이터 삭ㅈㅔ
    void deleteByMemberId(Long memberId);
}
