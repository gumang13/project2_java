package com.setting.repository;

import com.setting.entity.SettingPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettingPaymentRepository extends JpaRepository<SettingPayment, Long> {

    // 회원 ID로 결제 내역을 최신순으로 조회합니다.
    List<SettingPayment> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}