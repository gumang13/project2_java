package com.stretch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PoseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long sessionId;

    @Column
    private Long poseId;

    @Column
    private int resultOrder;

    @Column
    private boolean completed;

    @Column
    private int coachCount;

    @Column
    private int holdAchievedSec;
}
