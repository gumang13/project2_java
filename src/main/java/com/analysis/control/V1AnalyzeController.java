package com.analysis.control;

import com.security.dto.ApiResponse;
import com.setting.service.SettingApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class V1AnalyzeController {

    private final SettingApiKeyService apiKeyService;

    // FastAPI 주소 (env : FASTAPI_BASE_URL, 기본 http://localhost:8000)
    private final RestClient fastApiClient = RestClient.create();
    private static final String FASTAPI_BASE_URL =
            System.getenv().getOrDefault("FASTAPI_BASE_URL", "http://localhost:8000");

    // 외부 개발자용 단일 이미지 자세 분석
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Object> analyze(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("image") MultipartFile image
    ) {
        // 1) API 키 인증 (Authorization : Bearer ptk_...)
        try {
            String rawKey = extractBearer(authorization);
            Long memberId = apiKeyService.authenticateAndGetMemberId(rawKey);
            // memberId는 이후 사용량/분당 한도(429) 집계 지점에서 활용 - 직므은 인증만.
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }

        // 2) FastAPI 단일 분석 호출 (이미지 바이트를 그대로 전달)
        try {
            Object result = fastApiClient.post()
                    .uri(FASTAPI_BASE_URL + "/analysis/analyze-once")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(image.getBytes())
                    .retrieve()
                    .body(Object.class);

            return ApiResponse.success(result);
        } catch (IOException e) {
            return ApiResponse.error("이미지 읽기에 실패했습니다.");
        }
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization: Bearer <API_KEY> 헤더가 필요합니다.");
        }
        return authorization.substring(7).trim();
    }
}
