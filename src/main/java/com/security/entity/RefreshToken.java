package com.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Refresh Token을 발급받은 회원 id
    @Column(nullable = false)
    private Long memberId;

    // Refresh Token 원문은 저장하지 않고, 해시값만 DB에 저장
    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    // Refresh Token 만료 시간
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 로그아웃, 무활동 만료, 재발급 rotation 시 폐기 여부
    @Column(nullable = false)
    private boolean revoked = false;

    // 토큰 데이터 생성 시간
    @CreatedDate
    private LocalDateTime createdDate;

    // 토큰 데이터 마지막 수정 시간
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    // 현재 시간이 만료 시간을 지났는지 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // 토큰이 폐기되지 않았고 만료되지 않았는지 확인
    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    // 토큰 폐기 처리
    public void revoke() {
        this.revoked = true;
    }
}