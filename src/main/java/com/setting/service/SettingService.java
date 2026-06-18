package com.setting.service;

import com.setting.dto.SettingDto;
import com.setting.entity.Setting;
import com.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingService {

    private final SettingRepository settingRepository;

    @Transactional
    public void updateSetting(Long userId,String key,Object value) {

        Setting setting= settingRepository.findByUserId(userId).orElseThrow(() ->new IllegalAccessError("회원 정보가 없습니다"));
        switch (key){
        case "alertSound" -> setting.setAlertSound((Boolean) value);
        case "tts" -> setting.setTts((Boolean) value);
        case "badAlertSec" -> setting.setBadAlertSec((Integer) value);
        case "dedupAlert" -> setting.setDedupAlert((Boolean) value);
        case "screenToast" -> setting.setScreenToast((Boolean) value);
        case "osDesktop" -> setting.setOsDesktop((Boolean) value);
            default -> {
                System.out.println(key);
                throw new IllegalAccessError("키값이 일치하지 않습니다");

            }
        }
    }


    public SettingDto getSetting(Long userId){
        SettingDto settingDto = new SettingDto();
        Setting setting=settingRepository.findByUserId(userId).orElseThrow(() ->
                new RuntimeException("회원 정보를 찾을 수 없습니다"));
        settingDto.setScreenToast(setting.isScreenToast());
        settingDto.setBadAlertSec(setting.getBadAlertSec());
        settingDto.setTts(setting.isTts());
        settingDto.setDedupAlert(setting.isDedupAlert());
        settingDto.setAlertSound(setting.isAlertSound());
        settingDto.setOsDesktop(setting.isOsDesktop());

        return settingDto;
    }
}
