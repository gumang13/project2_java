package com.setting.dto;

import com.member.entity.Member;

import java.time.LocalDateTime;

public record AccountInfoResponse(
        Long memberId,
        String name,
        String email,
        String plan,
        LocalDateTime createdDate
) {
    // Member 엔티티를 계정관리 화면 응답 DTO로 바꿔주는 메서드입니다.
    // 비밀번호는 보안상 절대 응답에 포함하지 않습니다.
    public static AccountInfoResponse from(Member member) {
        return new AccountInfoResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPlan() == null ? null : member.getPlan().name(),
                member.getCreatedDate()
        );
    }
}
