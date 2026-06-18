package com.setting.dto;

// 계정관리 수정 API에서 공통으로 사용하는 요청값입니다.
// 이메일 수정은 email만 사용하고, 비밀번호 변경은 password/newPassword를 사용합니다.
public record AccountUpdateRequest(
        String email,
        String password,
        String newPassword
) {
}
