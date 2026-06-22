package com.analysis.repository;

import com.analysis.entity.AnalysisEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface AnalysisEventRepository extends JpaRepository<AnalysisEvent, Long> {

    List<AnalysisEvent> findByAnalysisIdIn(List<Long> analysisId);

    List<AnalysisEvent> findByAnalysisIdInAndEventAtBetween(
            List<Long> analysisIds, LocalDateTime from, LocalDateTime to);

}
