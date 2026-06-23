package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.SettingApiKeyCreateResponse;
import com.setting.dto.SettingApiKeyResponse;
import com.setting.service.SettingApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class SettingApiKeyController {
    private final SettingApiKeyService apiKeyService;

    // 새 API 키 발급. 원본 키는 이 응답에서만 1회 노출
    // 프론트가 /api/keys/로 (슬래시 없이) 호출하므로 매핑값을 비워둠.
    @PostMapping
    public ApiResponse<SettingApiKeyCreateResponse> issue(@AuthenticationPrincipal Long memberId) {
        System.out.println("나는 컨트롤");
        try {
            return ApiResponse.success(apiKeyService.issue(memberId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 내 API 키 목록 조회 (원본 키 미포함)
    @GetMapping
    public ApiResponse<List<SettingApiKeyResponse>> list(@AuthenticationPrincipal Long memberId) {
        try {
            return ApiResponse.success(apiKeyService.list(memberId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // API 키 폐기 (status = revoked). 본인 키만 폐기 가능
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> revoke(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        try {
            return ApiResponse.success(apiKeyService.revoke(memberId, id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
