package com.setting.dto;

import com.setting.entity.SettingSubscription;
import com.setting.enums.PlanType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettingSubscriptionResponse {

    // 구독 정보를 가진 회원 ID입니다.
    private Long memberId;

    // 구독 정보 ID입니다.
    private Long subscriptionId;

    // 현재 구독 요금제입니다.
    private PlanType plan;

    // 구독 시작 시간입니다.
    private LocalDateTime startedAt;

    // 구독 만료 시간입니다.
    private LocalDateTime expiresAt;

    // 연결된 결제 수단 ID입니다.
    private Long paymentMethodId;

    // 구독 정보 존재 여부입니다.
    private boolean hasSubscription;

    // 화면에 보여줄 안내 메시지입니다.
    private String message;

    public static SettingSubscriptionResponse from(SettingSubscription subscription) {
        return SettingSubscriptionResponse.builder()
                .memberId(subscription.getMemberId())
                .subscriptionId(subscription.getId())
                .plan(subscription.getPlan())
                .startedAt(subscription.getStartedAt())
                .expiresAt(subscription.getExpiresAt())
                .paymentMethodId(subscription.getPaymentMethodId())
                .hasSubscription(true)
                .message("구독 정보를 불러왔습니다.")
                .build();
    }

    public static SettingSubscriptionResponse empty(Long memberId) {
        return SettingSubscriptionResponse.builder()
                .memberId(memberId)
                .subscriptionId(null)
                .plan(PlanType.FREE)
                .startedAt(null)
                .expiresAt(null)
                .paymentMethodId(null)
                .hasSubscription(false)
                .message("등록된 구독 정보가 없어 무료 플랜으로 표시합니다.")
                .build();
    }
}
