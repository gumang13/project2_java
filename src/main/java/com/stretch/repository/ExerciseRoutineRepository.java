package com.stretch.repository;

import com.stretch.entity.ExerciseRoutine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseRoutineRepository extends JpaRepository<ExerciseRoutine, Long> {
    List<ExerciseRoutine> findByMemberIdIsNull();
    List<ExerciseRoutine> findByMemberIdOrderByUpdatedAtDesc(Long memberId);
    Optional<ExerciseRoutine> findByMemberIdAndSlotNo(Long memberId, Integer slotNo);
}
