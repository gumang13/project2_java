package com.setting.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.setting.entity.SettingCalibration;

import java.util.List;
import java.util.Optional;

public interface SettingCalibrationRepository extends JpaRepository<SettingCalibration, Long> {

    //member_id 값으로 캘리브레이션 정보 조회
    // camera_position이 추가되면서 의미를 잃음. 최종적으로 삭제되어야 할 메서드
    List<SettingCalibration> findAllByMemberId(Long memberId);

    // member_id + camera_position으로 캘리브레이션 정보 조회
    Optional<SettingCalibration> findByMemberIdAndCameraPosition(
            Long memberId,
            String cameraPosition
    );

    // 해당 member_id 의 캘리브레이션 정보가 있는지 확인
    boolean existsByMemberId(Long memberId);

    //정보 삭제 (정보 초기화 용)
    void deleteByMemberId(Long memberId);
}
