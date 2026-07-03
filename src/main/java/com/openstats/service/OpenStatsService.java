package com.openstats.service;

import com.openstats.dto.CohortResponse;
import com.openstats.dto.PostureSegmentResponse;
import com.openstats.entity.SegmentCohortStats;
import com.openstats.entity.SegmentPostureStats;
import com.openstats.repository.SegmentCohortStatsRepository;
import com.openstats.repository.SegmentPostureStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 오픈 통계 조회 서비스 — 미리 구운 집계 테이블만 읽는다 (런타임에 raw 데이터 미접근).
 * 최소표본 미만은 개인 재식별 방지·신뢰도 위해 "데이터 축적 중"으로 응답.
 */
@Service
@RequiredArgsConstructor
public class OpenStatsService {

    private final SegmentPostureStatsRepository postureRepository;
    private final SegmentCohortStatsRepository cohortRepository;

    // 실서비스 권장값은 5. 데모 데이터가 적어 시연용으로 2로 낮춤(발표 시 이 점을 언급).
    private static final int MIN_SAMPLE = 2;
    private static final String PERIOD_TYPE = "monthly";

    public PostureSegmentResponse posture(String ageGroup, String gender, String region, String period) {
        SegmentPostureStats s;
        if (period != null && !period.isBlank()) {
            s = postureRepository
                    .findFirstByAgeGroupAndGenderAndRegionAndPeriodTypeAndPeriodValue(
                            ageGroup, gender, region, PERIOD_TYPE, period)
                    .orElse(null);
        } else {
            List<SegmentPostureStats> list = postureRepository
                    .findByAgeGroupAndGenderAndRegionAndPeriodTypeOrderByPeriodValueDesc(
                            ageGroup, gender, region, PERIOD_TYPE);
            s = list.isEmpty() ? null : list.get(0);
        }

        PostureSegmentResponse.Segment seg =
                new PostureSegmentResponse.Segment(ageGroup, gender, region);

        if (s == null) {
            return new PostureSegmentResponse(seg, 0, period, false, null,
                    "해당 세그먼트 데이터가 아직 없습니다. (데이터 축적 중)");
        }
        if (s.getSampleSize() < MIN_SAMPLE) {
            return new PostureSegmentResponse(seg, s.getSampleSize(), s.getPeriodValue(), false, null,
                    "표본이 부족합니다 (최소 " + MIN_SAMPLE + "명). 데이터 축적 중.");
        }
        PostureSegmentResponse.PostureHealth health = new PostureSegmentResponse.PostureHealth(
                s.getAvgNeckAngle(),
                s.getTurtleNeckFreq(),
                new PostureSegmentResponse.StatusDistribution(
                        s.getStatusGood(), s.getStatusWatching(), s.getStatusAlert()));
        return new PostureSegmentResponse(seg, s.getSampleSize(), s.getPeriodValue(), true, health, null);
    }

    public CohortResponse cohort(String ageGroup, String gender) {
        List<SegmentCohortStats> rows =
                cohortRepository.findByAgeGroupAndGenderOrderByWeeksSinceStartAsc(ageGroup, gender);

        List<CohortResponse.CohortPoint> points = new ArrayList<>();
        for (SegmentCohortStats r : rows) {
            if (r.getSampleSize() >= MIN_SAMPLE) {
                points.add(new CohortResponse.CohortPoint(
                        r.getWeeksSinceStart(), r.getAvgNeckAngle(), r.getSampleSize()));
            }
        }
        CohortResponse.Segment seg = new CohortResponse.Segment(ageGroup, gender);
        if (points.isEmpty()) {
            return new CohortResponse(seg, false, points,
                    "표본 " + MIN_SAMPLE + "명 이상인 주차가 아직 없습니다. (종단 데이터 축적 중)");
        }
        return new CohortResponse(seg, true, points, null);
    }
}
