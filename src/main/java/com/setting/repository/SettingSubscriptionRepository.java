package com.setting.repository;

import com.setting.entity.SettingSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingSubscriptionRepository extends JpaRepository<SettingSubscription, Long> {

    // 회원 ID로 구독 정보를 조회합니다.
    Optional<SettingSubscription> findByMemberId(Long memberId);

    // 회원 ID에 해당하는 구독 정보가 있는지 확인합니다.
    boolean existsByMemberId(Long memberId);

    // 회원 ID로 구독 정보를 삭제합니다.
    void deleteByMemberId(Long memberId);
}