package com.stats.repository;

import com.stats.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {
    List<DailyStats> findByMemberIdAndStatDateBetween(Long memberId, LocalDate from, LocalDate to);
}
