package com.setting.entity;

import com.setting.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettingPayment {

    // 결제 내역 테이블의 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 결제한 회원 ID입니다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 결제 대상 구독 ID입니다.
    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    // 사용된 결제 수단 ID입니다.
    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;

    // 결제 금액입니다.
    @Column(name = "amount", nullable = false)
    private Integer amount;

    // 결제 성공, 실패, 취소 같은 결제 상태입니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    // PG 거래 ID를 흉내 내기 위한 값입니다.
    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    // 중복 결제를 막기 위한 요청 고유 키입니다.
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    // 결제가 완료된 시간입니다.
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 결제 내역이 생성된 시간입니다.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.READY;
        }
    }
}
