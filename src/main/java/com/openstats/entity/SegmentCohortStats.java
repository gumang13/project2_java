package com.openstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 개선 추이 코호트 집계 (연령대 × 성별 × 시작 후 N주차).
 * 지역은 제외 — 코호트+지역까지 쪼개면 표본이 붕괴하기 때문.
 */
@Entity
@Getter
@Setter
public class SegmentCohortStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ageGroup;     // "20s" | "30s" | ...
    private String gender;       // "male" | "female"
    private int weeksSinceStart; // 첫 사용 후 경과 주차 (0,1,2,...)

    private int sampleSize;      // DISTINCT 회원 수
    private Double avgNeckAngle; // 해당 주차 평균 경추 각도

    private LocalDateTime updatedAt;
}
