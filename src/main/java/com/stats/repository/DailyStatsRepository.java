package com.stats.repository;

import com.stats.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {
    List<DailyStats> findByMemberIdAndStatDateBetween(Long memberId, LocalDateTime from, LocalDateTime to);
}
