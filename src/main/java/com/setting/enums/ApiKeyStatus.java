package com.setting.enums;

import com.fasterxml.jackson.annotation.JsonValue;

// API 키 상태 - 활성(active) / 폐기(revoked)
// PaymentStatus와 달리 DB 기본값('active')과 프론트 비교값('revoked')이 소문자라,
// JSON 직렬화는 @JsonValue, DB 매핑은 ApiKeyStatusConverter로 소문자를 맞춘다.

public enum ApiKeyStatus {
    ACTIVE("active"),
    REVOKED("revoked");

    private final String code;

    ApiKeyStatus(String code) {
        this.code = code;
    }

    @JsonValue // JSON 응답 -> "active" / "revoked"
    public String getCode() {
        return code;
    }

    // DB 문자열 -> enum
    public static ApiKeyStatus fromCode(String code) {
        for (ApiKeyStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("알 수 없는  api_key status: " + code);
    }
}
