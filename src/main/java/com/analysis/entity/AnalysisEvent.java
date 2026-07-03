package com.analysis.entity;

import com.analysis.constant.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class AnalysisEvent {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long analysisId;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private LocalDateTime eventAt;

    // 오픈 통계 API용 경추 각도(도). 현재 파이프라인 미저장이라 nullable — 더미로 채워 집계에 사용.
    @Column
    private Double angle;

}
