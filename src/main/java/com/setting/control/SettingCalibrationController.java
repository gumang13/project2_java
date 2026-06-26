package com.setting.control;

import com.setting.dto.SettingCalibrationResponse;
import com.setting.service.SettingCalibrationService;
import com.security.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 캘리브레이션
import com.setting.dto.CalibrationSessionResultRequest;

// 캘리브레이션 세션 Dto
import com.setting.dto.CalibrationSessionStartResponse;



@RestController // JSON 응답을 반환하는 컨트롤러
@RequiredArgsConstructor // final 필드를 생성자로 자동 주입
@RequestMapping("/api/settings/calibration")
public class SettingCalibrationController {

    private final SettingCalibrationService settingCalibrationService;

    // 캘리브레이션 세션 생성
    @PostMapping("/sessions/start")
    public ApiResponse<CalibrationSessionStartResponse> startCalibrationSession(
            @AuthenticationPrincipal Long memberId
    ) {
        CalibrationSessionStartResponse response =
                settingCalibrationService.startCalibrationSession(memberId);

        return ApiResponse.success(response);
    }

    // 캘리브레션 저장
    @PostMapping("/sessions/result")
    public ApiResponse<String> saveCalibrationResult(
            @RequestBody CalibrationSessionResultRequest request
    ) {
        settingCalibrationService.saveCalibrationResult(
                request.calibrationSessionId(),
                request.cameraPosition(),
                request.goodAngle(),
                request.turtleThreshold()
        );

        return ApiResponse.success("캘리브레이션 결과가 저장되었습니다.");
    }

    // 분석 세션 id와 카메라 위치로 캘리브레이션 조회
    @GetMapping("/analysis/{analysisId}")
    public ApiResponse<SettingCalibrationResponse> getCalibrationForAnalysis(
            @PathVariable Long analysisId,
            @RequestParam String cameraPosition
    ) {
        SettingCalibrationResponse response =
                settingCalibrationService.getCalibrationForAnalysis(
                        analysisId,
                        cameraPosition
                );

        return ApiResponse.success(response);
    }

    // 캘리브레이션 기준값 조회
    @GetMapping("/")
    public ApiResponse<SettingCalibrationResponse> getCalibration(
            @AuthenticationPrincipal Long memberId
    ) {
        try {
            SettingCalibrationResponse response =
                    settingCalibrationService.getCalibration(memberId);

            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("캘리브레이션 정보를 불러오지 못했습니다.");
        }
    }


    // 캘리브레이션 재설정
    @DeleteMapping("/")
    public ApiResponse<String> resetCalibration(
            @AuthenticationPrincipal Long memberId
    ) {
        try {
            settingCalibrationService.resetCalibration(memberId);
            return ApiResponse.success("캘리브레이션 기준값이 초기화되었습니다.");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("캘리브레이션 초기화에 실패했습니다.");
        }
    }
}
