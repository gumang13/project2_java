package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.ExPostureBenchmarkResponse;
import com.setting.dto.ExPostureCoachingResponse;
import com.setting.dto.ExPostureDailyResponse;
import com.setting.service.ExPostureStatsService;
import com.setting.service.SettingApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 외부 개발자용 자세 건강 통계 API.
 * 키 인증으로 얻은 memberId를 "나"로 삼아, 익명 모집단과 비교한 개인화 통계를 제공한다.
 *
 *  GET /api/v1/posture-stats/benchmark  — 나 vs 전체 평균
 *  GET /api/v1/posture-stats/daily      — 나의 일자별 추이(+전체 평균 참조선)
 *  GET /api/v1/posture-stats/coaching   — 휴식·스트레칭 권장 판정
 *  인증 : Authorization: Bearer ptk_...
 */
@RestController
@RequestMapping("/api/v1/posture-stats")
@RequiredArgsConstructor
public class ExPostureStatsController {

    private final SettingApiKeyService apiKeyService;
    private final ExPostureStatsService statsService;

    @GetMapping("/benchmark")
    public ApiResponse<ExPostureBenchmarkResponse> benchmark(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long memberId;
        try {
            memberId = authenticate(authorization);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.success(statsService.benchmark(memberId, from, to));
    }

    @GetMapping("/daily")
    public ApiResponse<ExPostureDailyResponse> daily(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "perPage", defaultValue = "10") int perPage) {
        Long memberId;
        try {
            memberId = authenticate(authorization);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.success(statsService.daily(memberId, from, to, page, perPage));
    }

    @GetMapping("/coaching")
    public ApiResponse<ExPostureCoachingResponse> coaching(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "minNoti", defaultValue = "10") int minNoti) {
        Long memberId;
        try {
            memberId = authenticate(authorization);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.success(statsService.coaching(memberId, from, to, minNoti));
    }

    // Authorization: Bearer <API_KEY> → memberId
    private Long authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization: Bearer <API_KEY> 헤더가 필요합니다.");
        }
        String rawKey = authorization.substring(7).trim();
        return apiKeyService.authenticateAndGetMemberId(rawKey);
    }
}
