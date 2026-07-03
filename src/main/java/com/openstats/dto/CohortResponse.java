package com.openstats.dto;

import java.util.List;

/**
 * 오픈 API — 개선 추이(코호트) 응답.
 * 최소표본 이상인 주차만 내려감. 하나도 없으면 sufficient=false + "축적 중".
 */
public record CohortResponse(
        Segment segment,
        boolean sufficient,
        List<CohortPoint> cohortProgress,
        String message
) {
    public record Segment(String ageGroup, String gender) {}

    public record CohortPoint(int weeksSinceStart, double avgNeckAngle, int sampleSize) {}
}
