package com.setting.entity;

import com.member.constant.Plan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettingSubscription {

    // 구독 테이블의 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구독 정보를 가진 회원 ID입니다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 회원이 사용 중인 구독 요금제입니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    // 구독이 시작된 시간입니다.
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    // 구독이 만료되는 시간입니다.
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // 연결된 결제 수단 ID입니다.
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    // 구독 정보가 생성된 시간입니다.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (plan == null) {
            plan = Plan.FREE;
        }
    }
}
