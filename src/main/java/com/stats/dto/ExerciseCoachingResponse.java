package com.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExerciseCoachingResponse {

    // 자세명
    private String name;

    // 코칭 횟수 또는 기간 평균 코칭 횟수
    // 프론트 CoachingBars가 v
    private double v;
}
