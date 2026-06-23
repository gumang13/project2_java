package com.analysis.repository;

import com.analysis.entity.AnalysisEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface AnalysisEventRepository extends JpaRepository<AnalysisEvent, Long> {

    List<AnalysisEvent> findByAnalysisIdIn(List<Long> analysisId);

    List<AnalysisEvent> findByAnalysisIdInAndEventAtBetween(
            List<Long> analysisIds, LocalDateTime from, LocalDateTime to);

    //ByAnalysisId 필드 기준 전달 받은 id목록에 포함된 데이터 삭제후 반환없음
    //계정관리서비스 쪽 reset 관련
    void deleteByAnalysisIdIn(List<Long> analysisIds);
}
