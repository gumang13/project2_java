package com.analysis.control;

import com.analysis.dto.AnalysisStartResponse;
import com.analysis.service.AnalysisService;
import com.security.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

import com.analysis.dto.AnalysisSessionEndRequest;
import org.springframework.web.bind.annotation.RequestBody;

import com.analysis.dto.AnalysisEventSaveRequest; // 분석 이벤트 저장 요청

import com.analysis.dto.AnalysisEventLogResponse; // 분석 이벤트 조회 응답



@RestController
@RequestMapping("/api/analysis-sessions")
@RequiredArgsConstructor
public class AnalysisController {

    // 서비스 의존성 주입
    private final AnalysisService analysisService;

    // /start 핸들러 메서드
    @PostMapping("/start")
    public ApiResponse<AnalysisStartResponse> start(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.success(analysisService.start(memberId));
    }

    // 분석 세션 종료 요청
    @PostMapping("/end")
    public ApiResponse<Void> end(
            @RequestBody AnalysisSessionEndRequest request
    ) {
        analysisService.end(request);
        return ApiResponse.success(null);
    }

    // FastAPI 재시작 시 미종료 분석 세션 정리 요청
    @PostMapping("/cleanup-unfinished")
    public ApiResponse<Integer> cleanupUnfinishedSessions() {
        int cleanedCount = analysisService.endUnfinishedSessions();
        return ApiResponse.success(cleanedCount);
    }


    // 분석 이벤트 저장 요청
    @PostMapping("/events")
    public ApiResponse<Void> saveEvent(
            @RequestBody AnalysisEventSaveRequest request
    ) {
        analysisService.saveEvent(request);
        return ApiResponse.success(null);
    }

    // 오늘 이벤트 로그 조회 (프론트 우측 하단 표시용)
    @GetMapping("/events/today")
    public ApiResponse<List<AnalysisEventLogResponse>> todayEventLog(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.success(analysisService.getTodayEventLog(memberId));
    }

}
