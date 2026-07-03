package com.openstats.dto;

/**
 * 오픈 API — 세그먼트 자세 건강 응답.
 * 표본 부족 시 sufficient=false + message("축적 중"), postureHealth=null.
 */
public record PostureSegmentResponse(
        Segment segment,
        int sampleSize,
        String period,
        boolean sufficient,
        PostureHealth postureHealth,
        String message
) {
    public record Segment(String ageGroup, String gender, String region) {}

    public record PostureHealth(
            double avgNeckAngle,
            double turtleNeckFrequency,
            StatusDistribution statusDistribution
    ) {}

    public record StatusDistribution(double good, double watching, double alert) {}
}
