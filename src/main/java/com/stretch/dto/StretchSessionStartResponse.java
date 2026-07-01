package com.stretch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StretchSessionStartResponse {
    private Long sessionId;
    private Long routineId;
    private int totalPoses;
}