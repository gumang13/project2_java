package com.analysis.dto;

import com.analysis.constant.EventType;

import java.time.LocalDateTime;

public record AnalysisEventSaveRequest(
        Long analysisId,
        EventType eventType,
        LocalDateTime eventAt
) {
}