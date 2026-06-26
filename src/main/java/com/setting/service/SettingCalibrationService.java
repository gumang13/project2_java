package com.setting.service;

import com.setting.dto.SettingCalibrationResponse;
import com.setting.repository.SettingCalibrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 켈리브레이션 추가
import com.setting.entity.SettingCalibration;
import com.analysis.repository.AnalysisRepository; // 분석 세션id로 memberid 조회

// 켈리브레이션 세션 추가
import com.setting.dto.CalibrationSessionStartResponse;
import com.setting.entity.SettingCalibrationSession;
import com.setting.repository.SettingCalibrationSessionRepository;

@Service // Spring이 Service 객체로 관리하게 해줌
@RequiredArgsConstructor // final 필드를 생성자로 자동 주입
@Transactional(readOnly = true) // 기본은 조회 전용 트랜잭션
public class SettingCalibrationService {

    private final SettingCalibrationRepository settingCalibrationRepository;
    private final SettingCalibrationSessionRepository settingCalibrationSessionRepository;
    private final AnalysisRepository analysisRepository; // 분석 세션id로 memberid 조회

    // 회원 ID로 캘리브레이션 기준값 조회
    public SettingCalibrationResponse getCalibration(Long memberId) {
        return settingCalibrationRepository.findByMemberId(memberId)
                .map(SettingCalibrationResponse::from) // 데이터가 있으면 Response로 변환
                .orElseGet(() -> SettingCalibrationResponse.empty(memberId)); // 없으면 빈 응답
    }

    // JWT에서 꺼낸 memberId로 캘리브레이션 세션을 만들고, calibrationSessionId를 반환해
    @Transactional
    public CalibrationSessionStartResponse startCalibrationSession(Long memberId) {
        SettingCalibrationSession session = SettingCalibrationSession.builder()
                .memberId(memberId)
                .build();

        SettingCalibrationSession savedSession =
                settingCalibrationSessionRepository.save(session);

        return new CalibrationSessionStartResponse(savedSession.getId());
    }

    // 켈리브레이션 저장 메서드
    @Transactional
    public void saveCalibrationResult(
            Long calibrationSessionId,
            String cameraPosition,
            Float goodAngle,
            Float turtleThreshold
    ) {
        SettingCalibrationSession session =
                settingCalibrationSessionRepository.findById(calibrationSessionId)
                        .orElseThrow(() -> new IllegalArgumentException("캘리브레이션 세션을 찾을 수 없습니다."));

        Long memberId = session.getMemberId();

        settingCalibrationRepository.findByMemberIdAndCameraPosition(memberId, cameraPosition)
                .ifPresentOrElse(
                        calibration -> calibration.updateCalibration(goodAngle, turtleThreshold),
                        () -> {
                            SettingCalibration newCalibration = SettingCalibration.builder()
                                    .memberId(memberId)
                                    .cameraPosition(cameraPosition)
                                    .goodAngle(goodAngle)
                                    .turtleThreshold(turtleThreshold)
                                    .build();

                            settingCalibrationRepository.save(newCalibration);
                        }
                );

        session.complete(cameraPosition, goodAngle, turtleThreshold);
    }

    // 분석 세션 id와 camera_position으로 캘리브레이션 조회하는 메서드
    public SettingCalibrationResponse getCalibrationForAnalysis(
            Long analysisId,
            String cameraPosition
    ) {
        Long memberId = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("분석 세션을 찾을 수 없습니다."))
                .getMemberId();

        return settingCalibrationRepository
                .findByMemberIdAndCameraPosition(memberId, cameraPosition)
                .map(SettingCalibrationResponse::from)
                .orElseGet(() -> SettingCalibrationResponse.empty(memberId));
    }

    // 회원 ID로 캘리브레이션 기준값 삭제
    @Transactional // 삭제 작업이므로 readOnly 해제
    public void resetCalibration(Long memberId) {
        if (settingCalibrationRepository.existsByMemberId(memberId)) {
            settingCalibrationRepository.deleteByMemberId(memberId);
        }
    }
}