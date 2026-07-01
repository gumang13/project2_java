package com.stretch.dto;

import lombok.Getter;

@Getter
public class StretchPoseCompleteRequest {
    private String poseKey;
    private int resultOrder;
    private int coachCount;
    private int holdAchievedSec = 5;
    private boolean completed = true;
}