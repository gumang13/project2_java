package com.setting.dto;

import com.setting.entity.SettingPaymentMethod;
import com.setting.enums.PaymentMethodType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettingPaymentMethodResponse {

    // 결제 수단을 등록한 회원 ID입니다.
    private Long memberId;

    // 결제 수단 ID입니다.
    private Long paymentMethodId;

    // 결제 수단 종류입니다.
    private PaymentMethodType type;

    // 화면에 보여줄 마스킹 번호입니다.
    private String maskedNumber;

    // 기본 결제 수단 여부입니다.
    private Boolean isDefault;

    // 결제 수단 등록 시간입니다.
    private LocalDateTime createdAt;

    // 결제 수단 존재 여부입니다.
    private boolean hasPaymentMethod;

    // 화면에 보여줄 안내 메시지입니다.
    private String message;

    public static SettingPaymentMethodResponse from(SettingPaymentMethod paymentMethod) {
        return SettingPaymentMethodResponse.builder()
                .memberId(paymentMethod.getMemberId())
                .paymentMethodId(paymentMethod.getId())
                .type(paymentMethod.getType())
                .maskedNumber(paymentMethod.getMaskedNumber())
                .isDefault(paymentMethod.getIsDefault())
                .createdAt(paymentMethod.getCreatedAt())
                .hasPaymentMethod(true)
                .message("결제 수단 정보를 불러왔습니다.")
                .build();
    }

    public static SettingPaymentMethodResponse empty(Long memberId) {
        return SettingPaymentMethodResponse.builder()
                .memberId(memberId)
                .paymentMethodId(null)
                .type(null)
                .maskedNumber(null)
                .isDefault(false)
                .createdAt(null)
                .hasPaymentMethod(false)
                .message("등록된 결제 수단이 없습니다.")
                .build();
    }
}
