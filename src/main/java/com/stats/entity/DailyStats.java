package com.stats.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
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
}
