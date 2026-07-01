package com.stretch.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class StretchSessionStartRequest {
    private Long routineId;
    private String source;
    private List<String> poseKeys;
}