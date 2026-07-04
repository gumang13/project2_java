package com.setting.service;

import com.setting.dto.SettingEspDeviceResponse;
import com.setting.entity.SettingEspDevice;
import com.setting.repository.SettingEspDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingEspDeviceService {

    private final SettingEspDeviceRepository espDeviceRepository;

    // 내 기기 목록 (최신순)
    public List<SettingEspDeviceResponse> list(Long memberId) {
        return espDeviceRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(SettingEspDeviceResponse::from)
                .toList();
    }

    // 등록된 MAC 목록 (브라우저가 등록 후보에서 제외하는 용도)
    public List<String> registeredMacs() {
        return espDeviceRepository.findAllDeviceMacs();
    }

    // 기기 등록 : MAC 정규화 후 전역 중복이면 거부 (이미 소유된 MAC = 접근 거부)
    @Transactional
    public SettingEspDeviceResponse register(Long memberId, String deviceMac, String name) {
        String normalized = normalizeMac(deviceMac);
        if (normalized == null) {
            throw new IllegalArgumentException("기기 MAC 주소가 필요합니다.");
        }
        if (espDeviceRepository.existsByDeviceMac(normalized)) {
            throw new IllegalArgumentException("이미 다른 사용자가 등록한 기기입니다.");
        }
        SettingEspDevice saved = espDeviceRepository.save(
                SettingEspDevice.builder()
                        .memberId(memberId)
                        .deviceMac(normalized)
                        .name((name == null || name.isBlank()) ? defaultName(normalized) : name.trim())
                        .build()
        );
        return SettingEspDeviceResponse.from(saved);
    }

    // 이름 수정 : 내 기기가 아니면 조회 자체가 안 됨
    @Transactional
    public SettingEspDeviceResponse rename(Long memberId, Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("기기 이름이 필요합니다.");
        }
        SettingEspDevice device = espDeviceRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없습니다."));
        device.rename(name.trim());
        return SettingEspDeviceResponse.from(device);
    }

    // 삭제 : 내 기기가 아니면 조회 자체가 안 됨
    @Transactional
    public Boolean delete(Long memberId, Long id) {
        SettingEspDevice device = espDeviceRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없습니다."));
        espDeviceRepository.delete(device);
        return true;
    }

    // Phase 3 세션 소유 검증에서 재사용
    public boolean isOwner(Long memberId, String deviceMac) {
        String normalized = normalizeMac(deviceMac);
        if (normalized == null) return false;
        return espDeviceRepository.findByDeviceMac(normalized)
                .map(d -> d.getMemberId().equals(memberId))
                .orElse(false);
    }

    // ───────────── 내부 유틸 ─────────────

    // FastAPI normalize_device_mac 와 동일 규칙 (대문자, '-' -> ':')
    // MAC주소를 "표준 형태"로 통일
    private String normalizeMac(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase().replace("-", ":");
        return normalized.isEmpty() ? null : normalized;
    }

    // 이름 안 정했을 때 기본 이름 생성
    private String defaultName(String mac) {
        String tail = mac.length() >= 5 ? mac.substring(mac.length() - 5) : mac;
        return "ESP 카메라 " + tail; // ex) "ESP 카메라 1A:2B"
    }
}