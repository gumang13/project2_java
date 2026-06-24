package com.calibration.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalibrationSaveRequest {

    private Long memberId;
    private Double goodAngle;
}