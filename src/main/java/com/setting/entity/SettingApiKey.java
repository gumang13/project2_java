package com.setting.entity;

import com.setting.enums.ApiKeyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_key")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED) // JPA 용 기본 생성자
@AllArgsConstructor
@Builder
public class SettingApiKey {

    @Id // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment
    private Long id;

    // 캘리브레이션과 달리 회원당 여러 개 발급 가능 (unique = true 아님!)
    @Column(name = "member_id", nullable = false)
    private Long memberId; // 회원 ID

    @Column(name = "key_hash", nullable = false)
    private String keyHash; // 키 해시 (원본은 저장하지 않음)

    @Column(name = "status", nullable = false)
    @Builder.Default
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE; // 활성/폐기(Converter가 소문자 매핑)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt; // 마지막 사용 시각(키 검증 단계에서 갱신 예정)

    @Column(name = "key_prefix")
    private String keyPrefix;

    @PrePersist // DB에 처음 저장되기 전 자동 실행
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ApiKeyStatus.ACTIVE;
        }
    }

    // 키 폐기 (status = REVOKED) - 영속 상태에서 호출 시 dirty checking으로 UPDATE
    public void revoke() {
        this.status = ApiKeyStatus.REVOKED;
    }

    // 키 사용 기록 - 향후 키 검증 경로(Security 필터)에서 호출 예정, 현재 범위 밖.
    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
