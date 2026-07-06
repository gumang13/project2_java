package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.SettingEspDeviceRegisterRequest;
import com.setting.dto.SettingEspDeviceRenameRequest;
import com.setting.dto.SettingEspDeviceResponse;
import com.setting.dto.SettingEspDeviceVerifyRequest;
import com.setting.service.SettingEspDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class SettingEspDeviceController {

    private final SettingEspDeviceService espDeviceService;

    // 내 기기 목록 조회
    @GetMapping
    public ApiResponse<List<SettingEspDeviceResponse>> list(@AuthenticationPrincipal Long memberId) {
        try {
            return ApiResponse.success(espDeviceService.list(memberId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 등록된 MAC 목록 조회 (소유자 정보 없이 MAC 문자열만)
    @GetMapping("/registered-macs")
    public ApiResponse<List<String>> registeredMacs() {
        return ApiResponse.success(espDeviceService.registeredMacs());
    }

    // ESP 기기 소유 검증 — 현재 로그인 회원이 이 MAC을 지금 소유 중인가 (요가 적용 전 사용)
    @PostMapping("/verify")
    public ApiResponse<Boolean> verify(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SettingEspDeviceVerifyRequest request
    ) {
        return ApiResponse.success(espDeviceService.isOwner(memberId, request.deviceMac()));
    }

    // 기기 등록 (claim 또는 수동 MAC 입력)
    @PostMapping
    public ApiResponse<SettingEspDeviceResponse> register(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SettingEspDeviceRegisterRequest request
    ) {
        try {
            return ApiResponse.success(
                    espDeviceService.register(memberId, request.deviceMac(), request.name()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 기기 이름 수정
    @PatchMapping("/{id}")
    public ApiResponse<SettingEspDeviceResponse> rename(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestBody SettingEspDeviceRenameRequest request
    ) {
        try {
            return ApiResponse.success(espDeviceService.rename(memberId, id, request.name()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 기기 삭제
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        try {
            return ApiResponse.success(espDeviceService.delete(memberId, id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}