package com.openstats.control;

import com.openstats.dto.CohortResponse;
import com.openstats.dto.PostureSegmentResponse;
import com.openstats.service.OpenStatsService;
import com.security.dto.ApiResponse;
import com.setting.service.SettingApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 외부 개발자용 오픈 통계 API.
 *  GET /api/v1/stats/posture?ageGroup=&gender=&region=&period=  — 세그먼트 자세 건강
 *  GET /api/v1/stats/cohort?ageGroup=&gender=                    — 개선 추이(코호트)
 *  인증 : Authorization: Bearer ptk_...
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class OpenStatsController {

    private final SettingApiKeyService apiKeyService;
    private final OpenStatsService openStatsService;

    private static final Set<String> AGE_GROUPS = Set.of("under20", "20s", "30s", "40s", "50plus");
    private static final Set<String> GENDERS = Set.of("male", "female");

    @GetMapping("/posture")
    public ApiResponse<PostureSegmentResponse> posture(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("ageGroup") String ageGroup,
            @RequestParam("gender") String gender,
            @RequestParam("region") String region,
            @RequestParam(value = "period", required = false) String period) {
        try {
            authenticate(authorization);
            validate(ageGroup, gender, region);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.success(openStatsService.posture(ageGroup, gender, region, period));
    }

    @GetMapping("/cohort")
    public ApiResponse<CohortResponse> cohort(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("ageGroup") String ageGroup,
            @RequestParam("gender") String gender) {
        try {
            authenticate(authorization);
            validateEnum(ageGroup, gender);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.success(openStatsService.cohort(ageGroup, gender));
    }

    private void authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization: Bearer <API_KEY> 헤더가 필요합니다.");
        }
        apiKeyService.authenticateAndGetMemberId(authorization.substring(7).trim());
    }

    private void validateEnum(String ageGroup, String gender) {
        if (!AGE_GROUPS.contains(ageGroup)) {
            throw new IllegalArgumentException("허용되지 않은 ageGroup 입니다. (허용: " + AGE_GROUPS + ")");
        }
        if (!GENDERS.contains(gender)) {
            throw new IllegalArgumentException("허용되지 않은 gender 입니다. (허용: " + GENDERS + ")");
        }
    }

    private void validate(String ageGroup, String gender, String region) {
        validateEnum(ageGroup, gender);
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region 파라미터가 필요합니다.");
        }
    }
}
