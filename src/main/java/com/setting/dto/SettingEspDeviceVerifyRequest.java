package com.setting.dto;

// 요가 카메라 적용 전 소유 검증 요청 (MAC만 필요)
public record SettingEspDeviceVerifyRequest(String deviceMac) {
}