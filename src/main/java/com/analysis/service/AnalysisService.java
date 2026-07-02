package com.analysis.service;

import com.analysis.constant.EventType;
import com.analysis.dto.AnalysisStartResponse;
import com.analysis.entity.Analysis;
import com.analysis.repository.AnalysisRepository;
import com.stats.entity.DailyStats;
import com.stats.repository.DailyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import com.analysis.dto.AnalysisSessionEndRequest;
import org.springframework.transaction.annotation.Transactional;


// 이벤트 저장 메서드
import com.analysis.dto.AnalysisEventSaveRequest;
import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final DailyStatsRepository dailyStatsRepository;

    private final AnalysisRepository analysisRepository; // 분석 세션
    private final AnalysisEventRepository analysisEventRepository; // 분석 이벤트(good, bad posture)

    // DB에 분석 세션
    public AnalysisStartResponse start(Long memberId){
        Analysis analysis = new Analysis();

        analysis.setMemberId(memberId);
        analysis.setStartedAt(LocalDateTime.now());

        Analysis savedAnalysis = analysisRepository.save(analysis);

        return new AnalysisStartResponse(savedAnalysis.getId());
    }

    @Transactional
    public void end(AnalysisSessionEndRequest request) {
        Analysis analysis = analysisRepository.findById(request.analysisId())
                .orElseThrow(() -> new IllegalArgumentException("분석 세션을 찾을 수 없습니다."));

        if (analysis.getEndedAt() != null) {
            return;
        }

        analysis.end(request.endedAt());
        saveDailyStats(analysis);
    }

    // FastAPI 재시작 시 Spring Boot의 미종료 세션 정리 구현
    @Transactional
    public int endUnfinishedSessions() {
        List<Analysis> unfinishedSessions = analysisRepository.findByEndedAtIsNull();
        LocalDateTime endedAt = LocalDateTime.now();

        for (Analysis analysis : unfinishedSessions) {
            analysis.end(endedAt);
            saveDailyStats(analysis);
        }

        return unfinishedSessions.size();
    }

    @Transactional
    public void saveEvent(AnalysisEventSaveRequest request) {

        // 분석 세션 존재 검증
        analysisRepository.findById(request.analysisId())
                .orElseThrow(() -> new IllegalArgumentException("분석 세션을 찾을 수 없습니다."));

        AnalysisEvent event = new AnalysisEvent(); // 생성
        event.setAnalysisId(request.analysisId()); //
        event.setEventType(request.eventType());
        event.setEventAt(request.eventAt());

        analysisEventRepository.save(event);
    }

    private void saveDailyStats(Analysis analysis) {
        if (analysis.getStartedAt() == null || analysis.getEndedAt() == null) {
            return;
        }

        DailyStatValues values = calculateDailyStatValues(analysis);
        LocalDateTime statDate = analysis.getStartedAt().toLocalDate().atStartOfDay();

        DailyStats dailyStats = dailyStatsRepository
                .findByMemberIdAndStatDate(analysis.getMemberId(), statDate)
                .orElseGet(() -> DailyStats.create(analysis.getMemberId(), statDate));

        dailyStats.addMeasuredSeconds(
                values.alerts(),
                values.goodSeconds(),
                values.badSeconds(),
                values.totalSeconds()
        );
        dailyStatsRepository.save(dailyStats);
    }

    private DailyStatValues calculateDailyStatValues(Analysis analysis) {
        LocalDateTime startedAt = analysis.getStartedAt();
        LocalDateTime endedAt = analysis.getEndedAt();
        List<AnalysisEvent> events = analysisEventRepository.findByAnalysisIdIn(List.of(analysis.getId()));

        events.sort(Comparator.comparing(AnalysisEvent::getEventAt));

        int alerts = 0;
        long badSeconds = 0;
        LocalDateTime badStartedAt = null;

        for (AnalysisEvent event : events) {
            if (event.getEventAt() == null) {
                continue;
            }

            if (event.getEventType() == EventType.BAD_POSTURE) {
                if (isInAnalysisPeriod(event.getEventAt(), startedAt, endedAt)) {
                    alerts++;
                }
                if (badStartedAt == null) {
                    badStartedAt = event.getEventAt();
                }
            }

            if (event.getEventType() == EventType.GOOD_POSTURE && badStartedAt != null) {
                badSeconds += calculateClippedSeconds(badStartedAt, event.getEventAt(), startedAt, endedAt);
                badStartedAt = null;
            }
        }

        // 마지막 상태가 BAD인 채로 종료되면 현재 시간이 아니라 분석 종료 시각까지만 집계한다.
        // If the session ends while still BAD, count bad posture only up to endedAt.
        if (badStartedAt != null) {
            badSeconds += calculateClippedSeconds(badStartedAt, endedAt, startedAt, endedAt);
        }

        int totalSeconds = toSafeInt(ChronoUnit.SECONDS.between(startedAt, endedAt));
        int safeBadSeconds = Math.min(toSafeInt(badSeconds), totalSeconds);
        int goodSeconds = Math.max(0, totalSeconds - safeBadSeconds);

        return new DailyStatValues(alerts, goodSeconds, safeBadSeconds, totalSeconds);
    }

    private boolean isInAnalysisPeriod(LocalDateTime eventAt, LocalDateTime startedAt, LocalDateTime endedAt) {
        return !eventAt.isBefore(startedAt) && !eventAt.isAfter(endedAt);
    }

    private long calculateClippedSeconds(
            LocalDateTime from,
            LocalDateTime to,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        LocalDateTime clippedFrom = from.isAfter(startedAt) ? from : startedAt;
        LocalDateTime clippedTo = to.isBefore(endedAt) ? to : endedAt;

        return clippedTo.isAfter(clippedFrom) ? ChronoUnit.SECONDS.between(clippedFrom, clippedTo) : 0;
    }

    private int toSafeInt(long value) {
        return (int) Math.min(Math.max(0, value), Integer.MAX_VALUE);
    }

    private record DailyStatValues(
            int alerts,
            int goodSeconds,
            int badSeconds,
            int totalSeconds
    ) {
    }
}
