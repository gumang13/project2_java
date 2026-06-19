package com.setting.control;

import com.setting.dto.SettingPaymentMethodResponse;
import com.setting.dto.SettingPaymentResponse;
import com.setting.dto.SettingSubscriptionRequest;
import com.setting.dto.SettingSubscriptionResponse;
import com.setting.service.SettingSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SettingSubscriptionController {

    private final SettingSubscriptionService settingSubscriptionService;

    // 회원의 현재 구독 정보를 조회합니다.
    @GetMapping("/settings/subscription/{memberId}")
    public ResponseEntity<SettingSubscriptionResponse> getSubscription(
            @PathVariable Long memberId
    ) {
        SettingSubscriptionResponse response =
                settingSubscriptionService.getSubscription(memberId);

        return ResponseEntity.ok(response);
    }

    // 회원의 구독 요금제를 변경합니다.
    @PutMapping("/settings/subscription/{memberId}")
    public ResponseEntity<SettingSubscriptionResponse> updateSubscription(
            @PathVariable Long memberId,
            @Valid @RequestBody SettingSubscriptionRequest request
    ) {
        SettingSubscriptionResponse response =
                settingSubscriptionService.updateSubscription(memberId, request);

        return ResponseEntity.ok(response);
    }

    // 회원의 기본 결제 수단을 조회합니다.
    @GetMapping("/settings/payment-method/{memberId}")
    public ResponseEntity<SettingPaymentMethodResponse> getDefaultPaymentMethod(
            @PathVariable Long memberId
    ) {
        SettingPaymentMethodResponse response =
                settingSubscriptionService.getDefaultPaymentMethod(memberId);

        return ResponseEntity.ok(response);
    }

    // 회원의 결제 내역을 조회합니다.
    @GetMapping("/settings/payments/{memberId}")
    public ResponseEntity<List<SettingPaymentResponse>> getPayments(
            @PathVariable Long memberId
    ) {
        List<SettingPaymentResponse> response =
                settingSubscriptionService.getPayments(memberId);

        return ResponseEntity.ok(response);
    }

    // 회원의 구독 설정을 초기화합니다.
    @DeleteMapping("/settings/subscription/{memberId}")
    public ResponseEntity<Map<String, String>> resetSubscription(
            @PathVariable Long memberId
    ) {
        settingSubscriptionService.resetSubscription(memberId);

        return ResponseEntity.ok(Map.of(
                "message", "구독 설정이 초기화되었습니다.",
                "redirectUrl", "/settings"
        ));
    }
}
