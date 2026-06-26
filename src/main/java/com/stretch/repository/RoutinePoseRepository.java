package com.stretch.repository;

import com.stretch.entity.RoutinePose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutinePoseRepository extends JpaRepository<RoutinePose, Long> {
    List<RoutinePose> findByRoutineIdOrderByPoseOrder(Long routineId);
}
