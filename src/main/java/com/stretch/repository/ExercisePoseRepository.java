package com.stretch.repository;

import com.stretch.entity.ExercisePose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExercisePoseRepository extends JpaRepository<ExercisePose, Long> {
    Optional<ExercisePose> findByPoseKey(String poseKey);
    List<ExercisePose> findByPoseKeyIn(Collection<String> poseKeys);
}