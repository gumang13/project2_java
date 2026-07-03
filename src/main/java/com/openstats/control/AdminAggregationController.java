package com.openstats.control;

import com.openstats.service.AggregationService;
import com.security.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오픈 통계 집계 배치 수동 트리거 (발표/데모용).
 * 실서비스에서는 @Scheduled 또는 관리자 인증으로 보호 필요.
 *
 *  POST /api/v1/admin/aggregate  → 세그먼트/코호트 집계 테이블 재계산
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAggregationController {

    private final AggregationService aggregationService;

    @PostMapping("/aggregate")
    public ApiResponse<Integer> aggregate() {
        int rows = aggregationService.aggregate();
        return ApiResponse.success(rows); // 새로 구운 집계 행 수
    }
}
