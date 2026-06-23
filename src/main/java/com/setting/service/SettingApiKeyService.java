package com.setting.service;

import com.setting.dto.SettingApiKeyCreateResponse;
import com.setting.dto.SettingApiKeyResponse;
import com.setting.entity.SettingApiKey;
import com.setting.enums.ApiKeyStatus;
import com.setting.repository.SettingApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingApiKeyService {

    private final SettingApiKeyRepository apiKeyRepository;

    private static final int MAX_ACTIVE_KEYS = 5; // 회원당 활성화 키 상한 (예외 메시지의 N)
    private static final String KEY_PREFIX = "ptk_"; // 사용자가 키를 알아보기 쉽게 (선택)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 키 발급 : 원본 키는 반환값으로만 1회 전달, DB엔 해시만 저장
    @Transactional
    public SettingApiKeyCreateResponse issue(Long memberId) {

        long activeCount = apiKeyRepository.countByMemberIdAndStatus(memberId, ApiKeyStatus.ACTIVE);
        if (activeCount >= MAX_ACTIVE_KEYS) {
            throw new IllegalArgumentException("최대 " + MAX_ACTIVE_KEYS + "개까지만 발급할 수 있습니다.");
        }

        String rawKey = generateRawKey(); // 사용자에게 1회 노출할 원본 키
        String keyHash = sha256(rawKey); // DB엔 이 해시만 저장

        SettingApiKey saved = apiKeyRepository.save(
                SettingApiKey.builder()
                        .memberId(memberId)
                        .keyHash(keyHash)
                        .status(ApiKeyStatus.ACTIVE)
                        .build()
        );

        return SettingApiKeyCreateResponse.of(rawKey, saved);
    }

    // 내 키 목록 조회(원본 키 미포함)
    public List<SettingApiKeyResponse> list(Long memberId) {
        return apiKeyRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(SettingApiKeyResponse::from)
                .toList();
    }

    // 키 폐기 : 내 키가 아니면 조회 자체가 안 되므로 "접근 권한이 업습니다."로 처리
    @Transactional
    public Boolean revoke(Long memberId, Long keyId) {
        SettingApiKey key = apiKeyRepository.findByIdAndMemberId(keyId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없습니다."));
        key.revoke(); // 영속 상태 -> dirty checking으로 UPDATE
        return true;
    }

    // ───────────── 내부 유틸 ─────────────

    // 암호학적 난수 키 (256-bit). 일반 Random 금지, SecureRandom 사용.
    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // SHA-256 해시. 키는 고엔트로피 난수라 비밀번호와 달리 salt 없이도 안전.
    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest); // 64자 hex
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
