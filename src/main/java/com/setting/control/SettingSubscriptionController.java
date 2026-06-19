package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.SettingPaymentMethodResponse;
import com.setting.dto.SettingPaymentResponse;
import com.setting.dto.SettingSubscriptionRequest;
import com.setting.dto.SettingSubscriptionResponse;
import com.setting.service.SettingSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
public class SettingSubscriptionController {

    private final SettingSubscriptionService settingSubscriptionService;

    // 회원의 현재 구독 정보를 조회합니다.
    @GetMapping("/subscription/{memberId}")
    public ApiResponse<SettingSubscriptionResponse> getSubscription(
            @PathVariable Long memberId
    ) {
        try {
            SettingSubscriptionResponse response =
                    settingSubscriptionService.getSubscription(memberId);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("구독 정보를 불러오지 못했습니다.");
        }
    }

    // 회원의 구독 요금제를 변경합니다.
    @PutMapping("/subscription/{memberId}")
    public ApiResponse<SettingSubscriptionResponse> updateSubscription(
            @PathVariable Long memberId,
            @Valid @RequestBody SettingSubscriptionRequest request
    ) {
        try {
            SettingSubscriptionResponse response =
                    settingSubscriptionService.updateSubscription(memberId, request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("구독 요금제 변경에 실패했습니다.");
        }
    }

    // 회원의 기본 결제 수단을 조회합니다.
    @GetMapping("/payment-method/{memberId}")
    public ApiResponse<SettingPaymentMethodResponse> getDefaultPaymentMethod(
            @PathVariable Long memberId
    ) {
        try {
            SettingPaymentMethodResponse response =
                    settingSubscriptionService.getDefaultPaymentMethod(memberId);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("결제 수단 정보를 불러오지 못했습니다.");
        }
    }

    // 회원의 결제 내역을 조회합니다.
    @GetMapping("/payments/{memberId}")
    public ApiResponse<List<SettingPaymentResponse>> getPayments(
            @PathVariable Long memberId
    ) {
        try {
            List<SettingPaymentResponse> response =
                    settingSubscriptionService.getPayments(memberId);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("결제 내역을 불러오지 못했습니다.");
        }
    }

    // 회원의 구독 설정을 초기화합니다.
    @DeleteMapping("/subscription/{memberId}")
    public ApiResponse<Boolean> resetSubscription(
            @PathVariable Long memberId
    ) {
        try {
            settingSubscriptionService.resetSubscription(memberId);
            return ApiResponse.success(true);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("구독 설정 초기화에 실패했습니다.");
        }
    }
}
