package com.setting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calibration_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettingCalibrationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 캘리브레이션 각도 변화 추적용
    @Column(name = "camera_position")
    private String cameraPosition;

    @Column(name = "good_angle")
    private Float goodAngle;

    @Column(name = "turtle_threshold")
    private Float turtleThreshold;

    // 세션 시작할 때 자동으로 시작되는 메서드 ex) save()호출 시 작동
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.startedAt = now;
        this.createdAt = now;
    }

    public void complete(String cameraPosition, Float goodAngle, Float turtleThreshold) {
        this.cameraPosition = cameraPosition;
        this.goodAngle = goodAngle;
        this.turtleThreshold = turtleThreshold;
        this.completedAt = LocalDateTime.now();
    }


}
