package com.setting.control;

import com.analysis.constant.EventType;
import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;
import com.security.dto.ApiResponse;
import com.setting.dto.ExPostureDataListResponse;
import com.setting.service.SettingApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 외부 개발자용 API 키 기반 접근 컨트롤러
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExApiKeyController {

    private final SettingApiKeyService apiKeyService;
    private final AnalysisEventRepository analysisEventRepository;

    private static final int MAX_PER_PAGE = 100;

    /**
     * 외부 개발자용 자세 데이터 목록 조회
     * 누구의 자세인지는 밝히지 않고, 페이지 단위(기본 10개)로 판정 결과만 표시
     *
     * GET /api/v1/posture-data-list?page=1&perPage=10
     * 인증 : Authorization: Bearer ptk_...
     */
    @GetMapping("/posture-data-list")
    public ApiResponse<ExPostureDataListResponse> postureDataList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "perPage", defaultValue = "10") int perPage
    ) {
        // 1) API 키 인증 (Authorization : Bearer ptk_...)
        try {
            String rawKey = extractBearer(authorization);
            apiKeyService.authenticateAndGetMemberId(rawKey); // 인증만 수행 (개인 식별정보는 응답에 미노출)
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }

        // 2) 페이지 파라미터 정규화 (1-base → 0-base, 상한 제한)
        int safePage = Math.max(page, 1);
        int safePerPage = Math.min(Math.max(perPage, 1), MAX_PER_PAGE);

        // 3) 최신 순으로 페이징 조회 (memberId·analysisId 등 개인정보는 매핑에서 제외)
        Page<AnalysisEvent> found = analysisEventRepository.findAll(
                PageRequest.of(safePage - 1, safePerPage, Sort.by(Sort.Direction.DESC, "eventAt"))
        );

        List<ExPostureDataListResponse.Item> items = found.getContent().stream()
                .map(e -> new ExPostureDataListResponse.Item(
                        e.getId(),
                        toResult(e.getEventType()),
                        null, // cvaAngle: 현재 스키마 미저장 → null
                        e.getEventAt()
                ))
                .toList();

        ExPostureDataListResponse body = new ExPostureDataListResponse(
                safePage,
                safePerPage,
                found.getTotalElements(),
                items.size(),
                items
        );

        return ApiResponse.success(body);
    }

    // 이벤트 유형 → 외부 공개용 판정 문자열
    private String toResult(EventType type) {
        if (type == EventType.GOOD_POSTURE) return "normal";
        if (type == EventType.BAD_POSTURE) return "turtle_neck";
        return "unknown";
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization: Bearer <API_KEY> 헤더가 필요합니다.");
        }
        return authorization.substring(7).trim();
    }
}
