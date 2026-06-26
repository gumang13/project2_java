package com.stats.repository;

import com.stretch.entity.PoseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExercisePoseResultStatsRepository extends JpaRepository<PoseResult, Long> {

    // 통계 탭: 기간 내 세션들의 자세별 결과를 조회합니다.
    List<PoseResult> findBySessionIdIn(List<Long> sessionIds);

    // 통계 탭: 세션 하나의 자세별 결과를 조회합니다.
    List<PoseResult> findBySessionId(Long sessionId);
}
