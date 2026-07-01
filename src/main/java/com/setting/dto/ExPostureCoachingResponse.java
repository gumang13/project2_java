package com.setting.dto;

import java.util.List;

/**
 * 외부 API — 나(키 소유자) 대상 휴식·스트레칭 코칭 판정.
 * 내 좋은 자세율이 전체 평균보다 낮거나, 일 평균 알림이 임계값 이상이면 권장한다.
 */
public record ExPostureCoachingResponse(
        boolean needStretchBreak, // 휴식·스트레칭 권장 여부
        double myGoodRatio,       // 내 좋은 자세율(%)
        double populationAvg,     // 전체 평균 좋은 자세율(%)
        double myAvgNotiPerDay,   // 내 일 평균 알림 횟수
        int notiThreshold,        // 알림 임계값
        List<String> reasons,     // 판정 근거
        String recommendation     // 사용자에게 보낼 권장 메시지
) {
}
