package com.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExerciseTypeResponse {

    // 스트레칭 세션 수
    private int stretch;

    // 요가 세션 수
    private int yoga;
}