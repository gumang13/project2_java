package com.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Long memberId;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime endedAt;   // 진행 중이면 null

    @CreatedDate
    private LocalDateTime createdAt;
}
