package com.setting.dto;

import com.setting.entity.SettingEspDevice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettingEspDeviceResponse {

    private Long deviceId;
    private String deviceMac;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    public static SettingEspDeviceResponse from(SettingEspDevice device) {
        return SettingEspDeviceResponse.builder()
                .deviceId(device.getId())
                .deviceMac(device.getDeviceMac())
                .name(device.getName())
                .createdAt(device.getCreatedAt())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }
}