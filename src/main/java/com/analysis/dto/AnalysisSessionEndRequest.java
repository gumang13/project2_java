package com.analysis.dto;

import java.time.LocalDateTime;

public record AnalysisSessionEndRequest(
        Long analysisId,
        LocalDateTime endedAt
) {
}