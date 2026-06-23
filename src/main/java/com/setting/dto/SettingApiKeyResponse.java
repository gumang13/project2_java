package com.setting.dto;

import com.setting.entity.SettingApiKey;
import com.setting.enums.ApiKeyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 키 목록 응답 — 보안상 원본 키는 절대 포함하지 않는다.
@Getter
@Builder
public class SettingApiKeyResponse {

    private Long keyId; // 키 식별자
    private LocalDateTime createdAt; // 발급일
    private ApiKeyStatus status; // 활성/폐기 (@JsonValue로 "active"/"revoked")
    private String keyPrefix;
    private LocalDateTime lastUsedAt;


    public static SettingApiKeyResponse from(SettingApiKey key) {
        return SettingApiKeyResponse.builder()
                .keyId(key.getId())
                .createdAt(key.getCreatedAt())
                .status(key.getStatus())
                .keyPrefix(key.getKeyPrefix())
                .lastUsedAt(key.getLastUsedAt())
                .build();
    }
}
