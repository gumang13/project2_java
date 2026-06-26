package com.analysis.service;

import com.analysis.dto.AnalysisStartResponse;
import com.analysis.entity.Analysis;
import com.analysis.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        analysis.end(request.endedAt());
    }

    // FastAPI 재시작 시 Spring Boot의 미종료 세션 정리 구현
    @Transactional
    public int endUnfinishedSessions() {
        List<Analysis> unfinishedSessions = analysisRepository.findByEndedAtIsNull();
        LocalDateTime endedAt = LocalDateTime.now();

        for (Analysis analysis : unfinishedSessions) {
            analysis.end(endedAt);
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


}
