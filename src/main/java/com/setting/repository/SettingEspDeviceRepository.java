package com.setting.repository;

import com.setting.entity.SettingEspDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;   // ← import 추가

import java.util.List;
import java.util.Optional;

public interface SettingEspDeviceRepository extends JpaRepository<SettingEspDevice, Long> {

    // 내 기기 목록 (최신순)
    List<SettingEspDevice> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 이름수정·삭제 시 소유권 확인용 - 내 기기가 아니면 조회 안 됨
    Optional<SettingEspDevice> findByIdAndMemberId(Long id, Long memberId);

    // 전역 유일 검증 / 소유 검증용
    Optional<SettingEspDevice> findByDeviceMac(String deviceMac);
    boolean existsByDeviceMac(String deviceMac);

    // 등록된 모든 기기의 MAC만 조회 (소유자·이름 제외, 등록 여부 판별용)
    @Query("select d.deviceMac from SettingEspDevice d")
    List<String> findAllDeviceMacs();
}