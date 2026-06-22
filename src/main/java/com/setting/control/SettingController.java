package com.setting.control;

import com.security.dto.ApiResponse;
import com.setting.dto.SettingDto;
import com.setting.dto.SettingUpdateRequestDto;
import com.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/setting")
@RequiredArgsConstructor
public class SettingController{

    private final SettingService settingService;

    @GetMapping("/")
    public ApiResponse<SettingDto> getSetting(@AuthenticationPrincipal Long userId){
        SettingDto setting =settingService.getSetting(userId);
        return ApiResponse.success(setting);
    }
    @PostMapping("/")
    public ApiResponse<Void> setSetting(@AuthenticationPrincipal Long userId, @RequestBody SettingUpdateRequestDto req){
        settingService.updateSetting(userId,req.getKey(),req.getValue());
        return ApiResponse.success(null);

    }

}


