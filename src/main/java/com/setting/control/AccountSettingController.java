package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.AccountInfoResponse;
import com.setting.dto.AccountUpdateRequest;
import com.setting.service.AccountSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class AccountSettingController {

    private final AccountSettingService accountSettingService;

    // 계정관리 화면에 처음 들어왔을 때 회원 정보를 조회하는 API입니다.
    // 비밀번호는 응답에 포함하지 않고, 화면에 필요한 기본 정보만 내려줍니다.
    @GetMapping("/{memberId}")
    public ApiResponse<AccountInfoResponse> getAccountInfo(@PathVariable Long memberId) {
        try {
            return ApiResponse.success(accountSettingService.getAccountInfo(memberId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 계정관리 화면에서 이메일을 수정할 때 호출하는 API입니다.
    // 요청 body 예시: { "email": "new@example.com" }
    @PostMapping("/{memberId}/email")
    public ApiResponse<AccountInfoResponse> updateEmail(
            @PathVariable Long memberId,
            @RequestBody AccountUpdateRequest request
    ) {
        try {
            return ApiResponse.success(accountSettingService.updateEmail(memberId, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 계정관리 화면에서 비밀번호를 변경할 때 호출하는 API입니다.
    // password는 현재 비밀번호, newPassword는 새로 저장할 비밀번호입니다.
    // 요청 body 예시: { "password": "oldPw", "newPassword": "newPw" }
    @PostMapping("/{memberId}/password")
    public ApiResponse<String> updatePassword(
            @PathVariable Long memberId,
            @RequestBody AccountUpdateRequest request
    ) {
        try {
            return ApiResponse.success(accountSettingService.updatePassword(memberId, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
