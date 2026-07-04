package com.setting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "esp_device")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettingEspDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId; // 소유자

    // 전역 유일 → 한 ESP는 한 회원만 소유
    @Column(name = "device_mac", nullable = false, unique = true)
    private String deviceMac;

    @Column(name = "name")
    private String name; // 사용자 지정 이름

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt; // 정보용(등록 시각). 실시간 online 판정은 FastAPI가 담당

    @PrePersist // Prepersist는 생명주기 콜백 애노테이션(repository.save(entity) 호출 시 자동 실행
    protected void onCreate() { // createdAt = now() 세팅
        this.createdAt = LocalDateTime.now();
    }

    // 이름 수정 - 영속 상태에서 dirty checking으로 UPDATE
    public void rename(String name) {
        this.name = name;
    }

    public void markSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }
}