package com.setting.dto;

// 캘리브레이션 세션 시작 요청 (ESP 소유 검증용)
public record CalibrationSessionStartRequest(
        String source,      // "phone" 또는 "esp"
        String deviceMac
) {
}