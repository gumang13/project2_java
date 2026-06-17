package com.setting.entity;

import com.setting.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettingPaymentMethod {

    // 결제 수단 테이블의 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 결제 수단을 등록한 회원 ID입니다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 카드, 카카오, 토스 같은 결제 수단 종류입니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentMethodType type;

    // 화면에 보여줄 마스킹된 결제 번호입니다.
    @Column(name = "masked_number", length = 30)
    private String maskedNumber;

    // 기본 결제 수단인지 여부입니다.
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    // 결제 수단이 등록된 시간입니다.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
