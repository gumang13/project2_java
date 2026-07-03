package com.analysis.dto;

import com.analysis.constant.EventType;

import java.time.LocalDateTime;

public record AnalysisEventLogResponse(
        EventType eventType,   // BAD_POSTURE / GOOD_POSTURE
        LocalDateTime eventAt  // 이벤트 발생 시각
) {
}