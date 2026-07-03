package com.setting.dto;

import com.member.entity.Member;

import java.time.LocalDateTime;

public record AccountInfoResponse(
        Long memberId,
        String name,
        String email,
        String plan,
        Integer birthYear,
        String gender,
        String region,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        Double goodPostureRatio,
        Integer weeklyAlertCount,
        Integer usageDays,
        Boolean calibrationCompleted
) {
    // Member 엔티티와 계정관리 화면에 필요한 값을 응답 DTO로 바꿔주는 메서드입니다.
    // 비밀번호는 보안상 절대 응답에 포함하지 않습니다.
    public static AccountInfoResponse from(
            Member member,
            Double weeklyAverageScore,
            Integer weeklyAlertCount,
            Integer usageDays,
            Boolean calibrationCompleted
    ) {
        return new AccountInfoResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPlan() == null ? null : member.getPlan().name(),
                member.getBirthYear(),
                member.getGender(),
                member.getRegion(),
                member.getCreatedDate(),
                member.getLastModifiedDate(),
                weeklyAverageScore,
                weeklyAlertCount,
                usageDays,
                calibrationCompleted
        );
    }
}