package com.openstats.repository;

import com.openstats.entity.SegmentPostureStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SegmentPostureStatsRepository extends JpaRepository<SegmentPostureStats, Long> {

    Optional<SegmentPostureStats> findFirstByAgeGroupAndGenderAndRegionAndPeriodTypeAndPeriodValue(
            String ageGroup, String gender, String region, String periodType, String periodValue);

    // 최신 기간 조회용 (period 미지정 시)
    List<SegmentPostureStats> findByAgeGroupAndGenderAndRegionAndPeriodTypeOrderByPeriodValueDesc(
            String ageGroup, String gender, String region, String periodType);
}
