package com.stretch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ExercisePose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String nameKo;

    @Column
    private String nameEn;

    @Column
    private int holdSec;

    @Column
    private String introTts;

    @Column
    private boolean isBilateral;

    @CreatedDate
    private LocalDateTime createdAt;




}
