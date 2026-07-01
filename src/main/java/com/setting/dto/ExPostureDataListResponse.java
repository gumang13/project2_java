package com.setting.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 외부 개발자용 자세 데이터 목록 응답
 * 누구의 데이터인지(memberId 등)는 노출하지 않고, 페이지 단위로 자세 판정 결과만 제공
 */
public record ExPostureDataListResponse(
        int page,          // 현재 페이지 (1부터)
        int perPage,       // 페이지당 항목 수
        long totalCount,   // 전체 자세 데이터 수
        int currentCount,  // 이번 페이지에 실제로 담긴 항목 수
        List<Item> data    // 자세 데이터 목록
) {
    /**
     * 개별 자세 데이터 (간결형).
     * cvaAngle 은 현재 스키마(AnalysisEvent)에 저장되지 않아 null 로 내려감
     * 추후 각도 저장 컬럼이 추가되면 그대로 채워질 예정
     */
    public record Item(
            Long id,                 // 자세 데이터 식별자
            String result,           // 판정 결과: "normal" | "turtle_neck"
            Double cvaAngle,         // CVA 각도(도). 미저장 시 null
            LocalDateTime analyzedAt // 판정 시각
    ) {
    }
}
