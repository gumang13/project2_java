package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.AccountInfoResponse;
import com.setting.dto.AccountUpdateRequest;
import com.setting.service.AccountSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/")
    public ApiResponse<AccountInfoResponse> getAccountInfo(@AuthenticationPrincipal Long memberId) {
        try {
            return ApiResponse.success(accountSettingService.getAccountInfo(memberId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 계정관리 화면에서 이름을 수정할 때 호출하는 API입니다.
    // 요청 body 예시: { "name": "홍길동" }
    @PostMapping("/name")
    public ApiResponse<AccountInfoResponse> updateName(
            @AuthenticationPrincipal Long memberId,
            @RequestBody AccountUpdateRequest request
    ) {
        try {
            return ApiResponse.success(accountSettingService.updateName(memberId, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    //거주지 수정 api
    @PostMapping("/region")
    public ApiResponse<AccountInfoResponse> updateRegion(
            @AuthenticationPrincipal Long memberId,
            @RequestBody AccountUpdateRequest request
    ) {
        try {
            return ApiResponse.success(accountSettingService.updateRegion(memberId, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 계정관리 화면에서 이메일을 수정할 때 호출하는 API입니다.
    // 요청 body 예시: { "email": "new@example.com" }
    @PostMapping("/email")
    public ApiResponse<AccountInfoResponse> updateEmail(
            @AuthenticationPrincipal Long memberId,
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
    @PostMapping("/password")
    public ApiResponse<Boolean> updatePassword(
            @AuthenticationPrincipal Long memberId,
            @RequestBody AccountUpdateRequest request
    ) {
        try {
            return ApiResponse.success(accountSettingService.updatePassword(memberId, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    //통계 데이터 초기화
    //api/members/reset 요청보내면 service 에서 로그인한 아이디 정보 가져오고 resetStats 실행
    @DeleteMapping("/reset")
    public ApiResponse<Boolean> resetStats(@AuthenticationPrincipal Long memberId) {
        try {
            accountSettingService.resetStats(memberId);
            return ApiResponse.success(true);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    //회원 탈퇴 -로그인한 아이디 가져오고  서비스에서 탈퇴 로직 실행
    @DeleteMapping("/withdraw")
    public ApiResponse<Boolean> withdraw(@AuthenticationPrincipal Long memberId) {
        try {
            accountSettingService.withdraw(memberId);
            return ApiResponse.success(true);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
