package com.calibration.control;



import com.calibration.dto.CalibrationSaveRequest;
import com.calibration.service.CalibrationService;
import com.security.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calibration")
public class CalibrationControl {

    private final CalibrationService calibrationService;

    @PostMapping
    public ApiResponse<String> saveCalibration(
            @RequestBody CalibrationSaveRequest request
    ) {

        calibrationService.saveCalibration(
                request.getMemberId(),
                request.getGoodAngle()
        );

        return ApiResponse.success("저장 완료");
    }
}