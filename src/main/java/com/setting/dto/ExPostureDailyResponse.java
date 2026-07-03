package com.setting.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 외부 API — 나(키 소유자)의 일자별 자세 추이. 전체 평균을 참조선으로 함께 제공.
 */
public record ExPostureDailyResponse(
        int page,               // 현재 페이지(1부터)
        int perPage,            // 페이지당 항목 수
        long totalCount,        // 내 데이터가 있는 전체 일수
        int currentCount,       // 이번 페이지 항목 수
        double populationAvg,   // 전체 평균 좋은 자세율(%) — 추이 그래프 참조선
        List<Day> data          // 일자별 내 통계
) {
    public record Day(
            LocalDate date,     // 날짜
            double goodRatio,   // 그날 내 좋은 자세율(%)
            int measureMin,     // 그날 내 측정 시간(분)
            int notiCount       // 그날 내 알림 횟수
    ) {
    }
}
