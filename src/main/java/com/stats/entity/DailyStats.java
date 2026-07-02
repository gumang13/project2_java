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

    public static DailyStats create(Long memberId, LocalDateTime statDate) {
        DailyStats dailyStats = new DailyStats();
        dailyStats.memberId = memberId;
        dailyStats.statDate = statDate;
        return dailyStats;
    }

    // 같은 날짜에 분석을 여러 번 종료할 수 있으므로 일별 집계는 누적한다.
    public void addMeasuredSeconds(int alerts, int goodSeconds, int badSeconds, int totalSeconds) {
        this.notiCount += Math.max(0, alerts);
        this.goodPostureSec += Math.max(0, goodSeconds);
        this.badPostureSec += Math.max(0, badSeconds);
        this.totalMeasureSec += Math.max(0, totalSeconds);
    }

}
