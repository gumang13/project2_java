package com.setting.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// api_key.status 컬럼을 enum(ACTIVE/REVOKED) <-> 소문자 문자열(active/revoked)로 매핑한다.
// autoApply = true 라서 ApiKeyStatus 타입 필드에 자동 적용됨 (@Enumerated 불필요).

@Converter(autoApply = true)
public class ApiKeyStatusConverter implements AttributeConverter<ApiKeyStatus, String> {

    @Override
    public String convertToDatabaseColumn(ApiKeyStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public ApiKeyStatus convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : ApiKeyStatus.fromCode(dbValue);
    }
}
