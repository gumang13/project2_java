package com.stretch.repository;

import com.stretch.entity.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {


    List<ExerciseSession> findTop5ByMemberIdOrderByCreatedAtDesc(Long id);
}
