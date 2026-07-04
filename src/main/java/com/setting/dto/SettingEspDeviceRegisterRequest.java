package com.setting.dto;

// 기기 등록 요청 (claim 목록 선택 또는 MAC 수동 입력 모두 deviceMac 사용)
public record SettingEspDeviceRegisterRequest(
        String deviceMac,
        String name
) {
}