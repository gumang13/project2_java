package com.setting.service;

import com.setting.dto.SettingPaymentMethodResponse;
import com.setting.dto.SettingPaymentResponse;
import com.setting.dto.SettingSubscriptionRequest;
import com.setting.dto.SettingSubscriptionResponse;
import com.setting.entity.SettingSubscription;
import com.setting.enums.PlanType;
import com.setting.repository.SettingPaymentMethodRepository;
import com.setting.repository.SettingPaymentRepository;
import com.setting.repository.SettingSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingSubscriptionService {

    private final SettingSubscriptionRepository settingSubscriptionRepository;
    private final SettingPaymentMethodRepository settingPaymentMethodRepository;
    private final SettingPaymentRepository settingPaymentRepository;

    // 회원 ID로 현재 구독 정보를 조회합니다.
    public SettingSubscriptionResponse getSubscription(Long memberId) {
        return settingSubscriptionRepository.findByMemberId(memberId)
                .map(SettingSubscriptionResponse::from)
                .orElseGet(() -> SettingSubscriptionResponse.empty(memberId));
    }

    // 회원의 구독 요금제를 변경하거나 새로 저장합니다.
    @Transactional
    public SettingSubscriptionResponse updateSubscription(Long memberId, SettingSubscriptionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        SettingSubscription subscription = settingSubscriptionRepository.findByMemberId(memberId)
                .map(existing -> SettingSubscription.builder()
                        .id(existing.getId())
                        .memberId(existing.getMemberId())
                        .plan(request.getPlan())
                        .startedAt(existing.getStartedAt())
                        .expiresAt(calculateExpiresAt(request.getPlan(), now))
                        .paymentMethodId(request.getPaymentMethodId())
                        .createdAt(existing.getCreatedAt())
                        .build())
                .orElseGet(() -> SettingSubscription.builder()
                        .memberId(memberId)
                        .plan(request.getPlan())
                        .startedAt(now)
                        .expiresAt(calculateExpiresAt(request.getPlan(), now))
                        .paymentMethodId(request.getPaymentMethodId())
                        .createdAt(now)
                        .build());

        SettingSubscription savedSubscription = settingSubscriptionRepository.save(subscription);

        return SettingSubscriptionResponse.from(savedSubscription);
    }

    // 회원 ID로 기본 결제 수단을 조회합니다.
    public SettingPaymentMethodResponse getDefaultPaymentMethod(Long memberId) {
        return settingPaymentMethodRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .map(SettingPaymentMethodResponse::from)
                .orElseGet(() -> SettingPaymentMethodResponse.empty(memberId));
    }

    // 회원 ID로 결제 내역을 최신순으로 조회합니다.
    public List<SettingPaymentResponse> getPayments(Long memberId) {
        return settingPaymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(SettingPaymentResponse::from)
                .toList();
    }

    // 회원의 구독 정보를 삭제해 무료 플랜 상태로 돌립니다.
    @Transactional
    public void resetSubscription(Long memberId) {
        if (settingSubscriptionRepository.existsByMemberId(memberId)) {
            settingSubscriptionRepository.deleteByMemberId(memberId);
        }
    }

    // 무료 플랜은 만료일을 두지 않고, 유료 플랜은 한 달 뒤로 표시합니다.
    private LocalDateTime calculateExpiresAt(PlanType plan, LocalDateTime now) {
        if (plan == PlanType.FREE) {
            return null;
        }

        return now.plusMonths(1);
    }
}
