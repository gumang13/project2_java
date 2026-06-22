package com.setting.dto;

// 계정관리 수정 API에서 공통으로 사용하는 요청값입니다.
// 이름 수정은 name, 이메일 수정은 email, 비밀번호 변경은 password/newPassword를 사용합니다.
public record AccountUpdateRequest(
        String name,
        String email,
        String password,
        String newPassword
) {
}
