package com.stretch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PresetResponse {
    private Long routineId;
    private String name;
    private String description;
    private int estSeconds;
    private List<PoseResponse> poses;
}