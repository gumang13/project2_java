package com.setting.repository;

import com.setting.entity.SettingPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingPaymentMethodRepository extends JpaRepository<SettingPaymentMethod, Long> {

    // 회원 ID로 기본 결제 수단을 조회합니다.
    Optional<SettingPaymentMethod> findByMemberIdAndIsDefaultTrue(Long memberId);

    // 회원 ID에 등록된 결제 수단이 있는지 확인합니다.
    boolean existsByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
