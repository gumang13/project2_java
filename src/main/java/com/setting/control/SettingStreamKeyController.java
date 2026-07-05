// 목적: 로그인 회원의 휴대폰 RTMP 스트림 키를 내려주는 REST 컨트롤러.
//       프론트가 휴대폰 카메라 기본 주소(내부 Pull)와 사용자 안내용 Push 주소로 사용한다.
package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.SettingStreamKeyResponse;
import com.setting.service.SettingStreamKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/stream-key")
@RequiredArgsConstructor
public class SettingStreamKeyController {

    private final SettingStreamKeyService settingStreamKeyService;

    // 내 스트림 키 조회 (휴대폰 RTMP 회원별 격리용). JWT 로그인 회원 기준.
    @GetMapping
    public ApiResponse<SettingStreamKeyResponse> getStreamKey(@AuthenticationPrincipal Long memberId) {
        try {
            return ApiResponse.success(settingStreamKeyService.getStreamKey(memberId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
