package com.openstats.repository;

import com.openstats.entity.SegmentCohortStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SegmentCohortStatsRepository extends JpaRepository<SegmentCohortStats, Long> {

    List<SegmentCohortStats> findByAgeGroupAndGenderOrderByWeeksSinceStartAsc(String ageGroup, String gender);
}
