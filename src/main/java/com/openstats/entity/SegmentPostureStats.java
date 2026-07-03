package com.openstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 자세 건강 세그먼트 집계 (연령대 × 성별 × 지역 × 기간).
 * 배치가 미리 구워두는 캐시 테이블 — 오픈 API는 이걸 읽기만 한다.
 */
@Entity
@Getter
@Setter
public class SegmentPostureStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ageGroup;     // "20s" | "30s" | "40s" | "50plus"
    private String gender;       // "male" | "female"
    private String region;       // 시/도 단위 (예: "daejeon")
    private String periodType;   // "monthly"
    private String periodValue;  // "2026-06"

    private int sampleSize;              // 세그먼트 내 DISTINCT 회원 수
    private Double avgNeckAngle;         // 평균 경추 각도(도)
    private Double turtleNeckFreq;       // 세션당 거북목(alert) 감지 횟수
    private Double statusGood;           // 상태 분포 (합 = 1.0)
    private Double statusWatching;
    private Double statusAlert;

    private LocalDateTime updatedAt;
}
