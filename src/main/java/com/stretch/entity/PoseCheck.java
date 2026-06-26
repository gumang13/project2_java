package com.stretch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PoseCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long poseId;

    @Column
    private int checkOrder;

    @Column
    private String label;

    @Column
    private String coachMessage;

    @Column
    private float targetMin;

    @Column
    private float targetMax;
}
