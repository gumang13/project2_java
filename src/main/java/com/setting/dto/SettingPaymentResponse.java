package com.setting.dto;

import com.setting.entity.SettingPayment;
import com.setting.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettingPaymentResponse {

    // 결제 내역 ID입니다.
    private Long paymentId;

    // 결제한 회원 ID입니다.
    private Long memberId;

    // 결제 대상 구독 ID입니다.
    private Long subscriptionId;

    // 사용된 결제 수단 ID입니다.
    private Long paymentMethodId;

    // 결제 금액입니다.
    private Integer amount;

    // 결제 상태입니다.
    private PaymentStatus status;

    // PG 거래 ID입니다.
    private String pgTransactionId;

    // 결제 완료 시간입니다.
    private LocalDateTime paidAt;

    // 결제 내역 생성 시간입니다.
    private LocalDateTime createdAt;

    public static SettingPaymentResponse from(SettingPayment payment) {
        return SettingPaymentResponse.builder()
                .paymentId(payment.getId())
                .memberId(payment.getMemberId())
                .subscriptionId(payment.getSubscriptionId())
                .paymentMethodId(payment.getPaymentMethodId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .pgTransactionId(payment.getPgTransactionId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
