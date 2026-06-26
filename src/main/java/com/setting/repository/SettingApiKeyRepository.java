package com.setting.repository;

import com.setting.entity.SettingApiKey;
import com.setting.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingApiKeyRepository extends JpaRepository<SettingApiKey, Long> {

    // 내 키 목록 조회 (최신순)
    List<SettingApiKey> findByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId,ApiKeyStatus status);

    // 폐기 시 소유권 확인용 - 내 키가 아니면 조회 안 됨(타인 키 폐기 차단)
    Optional<SettingApiKey> findByIdAndMemberId(Long id, Long memberId);

    // 발급 개수 제한 체크 (활성 키 개수)
    long countByMemberIdAndStatus(Long memberId, ApiKeyStatus status);

    // (향후 키 검증 경로에서 아용 예정) 키 해시 + 활성 상태로 조회
    Optional<SettingApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

}
