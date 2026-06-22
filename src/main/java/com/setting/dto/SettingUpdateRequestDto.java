package com.setting.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SettingUpdateRequestDto {
    private String key;
    private Object value;
}
