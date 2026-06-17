package com.setting.controller;

import com.setting.dto.SettingCalibrationResponse;
import com.setting.service.SettingCalibrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController // JSON 응답을 반환하는 컨트롤러
@RequiredArgsConstructor // final 필드를 생성자로 자동 주입
public class SettingCalibrationController {

    private final SettingCalibrationService settingCalibrationService;

    // 캘리브레이션 기준값 조회
    @GetMapping("/settings/{memberId}/calibration")
    public ResponseEntity<SettingCalibrationResponse> getCalibration(
            @PathVariable Long memberId
    ) {
        SettingCalibrationResponse response =
                settingCalibrationService.getCalibration(memberId);

        return ResponseEntity.ok(response);
    }

    // 캘리브레이션 재설정
    @DeleteMapping("/settings/{memberId}/calibration")
    public ResponseEntity<Map<String, String>> resetCalibration(
            @PathVariable Long memberId
    ) {
        settingCalibrationService.resetCalibration(memberId);

        return ResponseEntity.ok(Map.of(
                "message", "캘리브레이션 기준값이 초기화되었습니다.",
                "redirectUrl", "/calibration"
        ));
    }
}