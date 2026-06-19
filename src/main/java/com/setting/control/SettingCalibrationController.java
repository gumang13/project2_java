package com.setting.control;

import com.setting.dto.SettingCalibrationResponse;
import com.setting.service.SettingCalibrationService;
import com.security.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



@RestController // JSON 응답을 반환하는 컨트롤러
@RequiredArgsConstructor // final 필드를 생성자로 자동 주입
@RequestMapping("/api/settings/calibration")
public class SettingCalibrationController {

    private final SettingCalibrationService settingCalibrationService;

    // 캘리브레이션 기준값 조회
    @GetMapping("/{memberId}")
    public ApiResponse<SettingCalibrationResponse> getCalibration(
            @PathVariable Long memberId
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
    @DeleteMapping("/{memberId}")
    public ApiResponse<String> resetCalibration(
            @PathVariable Long memberId
    ) {
        try {
            settingCalibrationService.resetCalibration(memberId);
            return ApiResponse.success("캘리브레이션 기준값이 초기화되었습니다.");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("캘리브레이션 초기화에 실패했습니다.");
        }
    }
}
