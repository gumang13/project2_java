package com.stats.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long memberId;

    @Column
    private LocalDateTime statDate;

    @Column
    private int notiCount;

    @Column
    private int goodPostureSec;

    // 거북목 자세 누적 시간(초) 컬럼 추가
    @Column
    private int badPostureSec;

    // 전체 측정 시간(초)  컬럼 추가
    @Column
    private int totalMeasureSec;




}
