package com.setting.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingCalibrationRequest {
    private Long analysisId;

    private String cameraPosition;

    private Float goodAngle;

    private Float turtleThreshold;
}
