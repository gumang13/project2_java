package com.stretch.repository;

import com.stretch.entity.PoseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PoseResultRepository extends JpaRepository<PoseResult, Long> {
    Optional<PoseResult> findBySessionIdAndResultOrder(Long sessionId, int resultOrder);
    int countBySessionId(Long sessionId);
}