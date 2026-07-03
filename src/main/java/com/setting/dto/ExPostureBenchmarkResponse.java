package com.setting.dto;

import java.time.LocalDate;

/**
 * 외부 API — 나(키 소유자) vs 전체 평균 벤치마크.
 * 개인 데이터는 호출자 본인 것만, 나머지는 익명 집계만 노출한다.
 */
public record ExPostureBenchmarkResponse(
        LocalDate from,          // 집계 기간 시작
        LocalDate to,            // 집계 기간 끝
        boolean hasData,         // 내 데이터가 기간 내 존재하는지
        double myGoodRatio,      // 내 좋은 자세율(%)
        double populationAvg,    // 전체 평균 좋은 자세율(%)
        double diff,             // 내 값 - 평균 (%p)
        int percentile,          // 전체에서 내 백분위(0~100)
        int sampleMembers        // 집계에 포함된 익명 회원 수
) {
}
