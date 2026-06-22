package com.setting.dto;

import com.setting.entity.SettingApiKey;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 키 발급 응답 — 원본 키(apiKey)는 발급 직후 "이 응답에서 단 한 번만" 내려간다.
// 이후 어떤 조회 API에도 원본 키는 포함되지 않는다.

@Getter
@Builder
public class SettingApiKeyCreateResponse {

    private String apiKey; // 원본 (1회 노출용)
    private Long keyId; // 키 식별자
    private LocalDateTime createdAt; // 발급일

    public static SettingApiKeyCreateResponse of (String rawKey, SettingApiKey key) {
        return SettingApiKeyCreateResponse.builder()
                .apiKey(rawKey)
                .keyId(key.getId())
                .createdAt(key.getCreatedAt())
                .build();
    }
}
