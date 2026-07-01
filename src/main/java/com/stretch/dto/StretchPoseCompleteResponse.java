package com.stretch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StretchPoseCompleteResponse {
    private Long sessionId;
    private int completedPoses;
    private int totalPoses;
    private boolean sessionCompleted;
}