package com.stats.control;

import com.security.dto.ApiResponse;
import com.stats.dto.MonitorStatsDto;
import com.stats.service.MonitorStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class MonitorStatsController {

    private final MonitorStatsService monitorStatsService;

    // 로그인한 사용자의 기간별 거북목 통계를 조회
    @GetMapping("/monitor")
    public ApiResponse<MonitorStatsDto> getMonitorStats(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String period
    ){
        try{
            return ApiResponse.success(
                    monitorStatsService.getMonitorStats(period, memberId)
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
