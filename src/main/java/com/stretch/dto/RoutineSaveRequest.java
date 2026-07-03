package com.stretch.dto;

import com.stretch.constant.ExerciseCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class RoutineSaveRequest {
    private Long routineId;

    @NotBlank
    private String name;

    private String description;

    private ExerciseCategory category = ExerciseCategory.STRETCH;

    private int difficulty = 1;

    private Integer slotNo;

    private List<Long> poseIds;

    private List<String> poseKeys;
}
