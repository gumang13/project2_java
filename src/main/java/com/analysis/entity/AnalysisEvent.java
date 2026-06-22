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


}
