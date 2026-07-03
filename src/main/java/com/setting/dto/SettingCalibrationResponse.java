package com.setting.dto;

import lombok.Builder;
import lombok.Getter;
import com.setting.entity.SettingCalibration;

@Getter
@Builder
public class SettingCalibrationResponse {

    private Long memberId; // 회원 ID

    private Long calibrationId; // 캘리브레이션 ID

    private String cameraPosition;

    private Float goodAngle; // 정자세 기준 각도

    private Float turtleThreshold; // 거북목 판정 임계 각도

    private boolean hasCalibration; // 캘리브레이션 등록 여부

    private String message; // 안내 메시지

    // DB에 캘리브레이션 데이터가 있을 때 사용
    public static SettingCalibrationResponse from(SettingCalibration calibration) {
        return SettingCalibrationResponse.builder()
                .memberId(calibration.getMemberId())
                .calibrationId(calibration.getId())
                .cameraPosition(calibration.getCameraPosition())
                .goodAngle(calibration.getGoodAngle())
                .turtleThreshold(calibration.getTurtleThreshold())
                .hasCalibration(true)
                .message("캘리브레이션 기준값을 불러왔습니다.")
                .build();
    }

    // DB에 캘리브레이션 데이터가 없을 때 사용
    public static SettingCalibrationResponse empty(Long memberId) {
        return SettingCalibrationResponse.builder()
                .memberId(memberId)
                .calibrationId(null)
                .goodAngle(null)
                .turtleThreshold(null)
                .hasCalibration(false)
                .message("저장된 캘리브레이션 기준값이 없습니다.")
                .build();
    }
}