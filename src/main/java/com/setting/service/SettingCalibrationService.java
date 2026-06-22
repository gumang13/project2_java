package com.setting.service;

import com.setting.dto.SettingCalibrationResponse;
import com.setting.repository.SettingCalibrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // Spring이 Service 객체로 관리하게 해줌
@RequiredArgsConstructor // final 필드를 생성자로 자동 주입
@Transactional(readOnly = true) // 기본은 조회 전용 트랜잭션
public class SettingCalibrationService {

    private final SettingCalibrationRepository settingCalibrationRepository;

    // 회원 ID로 캘리브레이션 기준값 조회
    public SettingCalibrationResponse getCalibration(Long memberId) {
        return settingCalibrationRepository.findByMemberId(memberId)
                .map(SettingCalibrationResponse::from) // 데이터가 있으면 Response로 변환
                .orElseGet(() -> SettingCalibrationResponse.empty(memberId)); // 없으면 빈 응답
    }

    // 회원 ID로 캘리브레이션 기준값 삭제
    @Transactional // 삭제 작업이므로 readOnly 해제
    public void resetCalibration(Long memberId) {
        if (settingCalibrationRepository.existsByMemberId(memberId)) {
            settingCalibrationRepository.deleteByMemberId(memberId);
        }
    }
}