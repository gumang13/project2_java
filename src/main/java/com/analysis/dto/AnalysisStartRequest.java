package com.analysis.dto;

// 분석 세션 시작 요청 (ESP 소유 검증용)
public record AnalysisStartRequest(
        String source,      // "phone" 또는 "esp"
        String deviceMac
) {
}