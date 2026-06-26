package com.setting.dto;

// FastAPI가 나중에 Spring Boot로 보낼 데이터 담는 그릇
public record CalibrationSessionResultRequest(
        Long calibrationSessionId,
        String cameraPosition,
        Float goodAngle,
        Float turtleThreshold
) {
}
