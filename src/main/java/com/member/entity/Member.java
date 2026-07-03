package com.member.entity;

import com.member.constant.Plan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String password;
    @Column(unique =true)
    private String email;
    @Enumerated(EnumType.STRING)
    private Plan plan;

    // 인구통계(오픈 통계 API 세그먼트용) — 동의 하에 수집. 주민등록번호 아님.
    @Column
    private Integer birthYear;   // 출생 연도 (연령대 산출)
    @Column
    private String gender;       // "male" | "female"
    @Column
    private String region;       // 시/도 단위 코드 (예: "daejeon") — 재식별 방지

    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;



}
