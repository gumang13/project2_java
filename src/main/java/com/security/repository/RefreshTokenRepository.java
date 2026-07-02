package com.security.repository;

import com.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 쿠키로 들어온 Refresh Token을 해시 처리한 뒤 DB에서 찾을 때 사용
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 특정 회원의 Refresh Token을 모두 삭제할 때 사용
    // 예: 로그인 시 기존 세션 초기화, 회원 탈퇴, 강제 로그아웃
    void deleteByMemberId(Long memberId);
}