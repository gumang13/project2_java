package com.stretch.dto;

import com.stretch.entity.ExerciseSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RecentSessionResponse {
    private Long sessionId;
    private String routineName;
    private LocalDateTime createdAt;
    private int totalPoses;
    private int completedPoses;

    public static RecentSessionResponse from(ExerciseSession s, String routineName) {
        return RecentSessionResponse.builder()
                .sessionId(s.getId())
                .routineName(routineName)
                .createdAt(s.getCreatedAt())
                .totalPoses(s.getTotalPoses())
                .completedPoses(s.getCompletedPoses())
                .build();
    }
}
