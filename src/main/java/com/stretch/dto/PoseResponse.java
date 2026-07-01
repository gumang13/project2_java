package com.stretch.dto;

import com.stretch.entity.ExercisePose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PoseResponse {
    private Long poseId;
    private String poseKey;
    private String nameKo;
    private String nameEn;
    private int holdSec;

    public static PoseResponse from(ExercisePose pose) {
        return PoseResponse.builder()
                .poseId(pose.getId())
                .poseKey(pose.getPoseKey())
                .nameKo(pose.getNameKo())
                .nameEn(pose.getNameEn())
                .holdSec(pose.getHoldSec())
                .build();
    }
}