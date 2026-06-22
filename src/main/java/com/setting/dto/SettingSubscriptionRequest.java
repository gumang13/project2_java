package com.setting.dto;

import com.member.constant.Plan;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SettingSubscriptionRequest {

    // 변경할 구독 요금제입니다.
    @NotNull
    private Plan plan;

    // 사용할 결제 수단 ID입니다.
    private Long paymentMethodId;
}
