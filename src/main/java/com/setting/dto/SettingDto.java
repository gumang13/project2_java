package com.setting.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingDto {

    private boolean screenToast;
    private boolean osDesktop;
    private boolean alertSound;
    private boolean tts;
    private int badAlertSec;
    private boolean dedupAlert;

}
