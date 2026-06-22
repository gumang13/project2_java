package com.stats.service;

import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;
import com.analysis.repository.AnalysisRepository;
import com.stats.dto.MonitorStatsDto;
import com.stats.entity.DailyStats;
import com.stats.repository.DailyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorStatsService {

    private final AnalysisRepository analysisRepository;
    private final AnalysisEventRepository analysisEventRepository;
    private final DailyStatsRepository dailyStatsRepository;

    public MonitorStatsDto getMonitorStats(String period,Long id) {
        MonitorStatsDto monitorStatsDto = new MonitorStatsDto();

        if (period.equals("today")) {
            // today 경로: analysis_events
            List<Long> analysisId = analysisRepository.findIdsByMemberId(id);
            if (analysisId.isEmpty()) {
                return monitorStatsDto;
            }
            LocalDateTime from = LocalDate.now().atStartOfDay();
            LocalDateTime to = LocalDateTime.now();
            List<AnalysisEvent> events =
                    analysisEventRepository.findByAnalysisIdInAndEventAtBetween(analysisId, from, to);
            // events 집계 → DTO

        } else {
            // weekly·monthly 경로: daily_stats (LocalDate)
            LocalDate fromDate = switch (period) {
                case "weekly" -> LocalDate.now().minusDays(6);
                case "monthly" -> LocalDate.now().minusDays(29);
                default -> throw new IllegalArgumentException("period 가 일치하지 않습니다");
            };
            LocalDate toDate = LocalDate.now();
            List<DailyStats> stats =
                    dailyStatsRepository.findByMemberIdAndStatDateBetween(id, fromDate, toDate);
            // stats 집계 → DTO

        }

        return monitorStatsDto;

    }
}
