package com.stretch.repository;

import com.stretch.entity.ExerciseRoutine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRoutineRepository extends JpaRepository<ExerciseRoutine, Long> {
    List<ExerciseRoutine> findByMemberIdIsNull();
}
