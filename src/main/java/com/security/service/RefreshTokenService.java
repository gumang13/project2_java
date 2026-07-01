package com.security.service;

import com.security.entity.RefreshToken;
import com.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_VALID_HOURS = 12;
    private static final int REFRESH_TOKEN_BYTE_SIZE = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // 새 Refresh Token 원문을 만들고, DB에는 해시값만 저장한 뒤 원문을 반환
    public String issue(Long memberId) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setMemberId(memberId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusHours(REFRESH_TOKEN_VALID_HOURS));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    // 쿠키로 들어온 Refresh Token 원문이 유효한지 확인
    @Transactional(readOnly = true)
    public RefreshToken validate(String rawToken) {
        String tokenHash = hash(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isUsable()) {
            throw new IllegalArgumentException("Expired or revoked refresh token");
        }

        return refreshToken;
    }

    // 기존 Refresh Token을 폐기하고 새 Refresh Token 발급
    public String rotate(String rawToken) {
        RefreshToken refreshToken = validate(rawToken);
        refreshToken.revoke();

        return issue(refreshToken.getMemberId());
    }

    // 로그아웃 또는 무활동 로그아웃 시 현재 Refresh Token 폐기
    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(RefreshToken::revoke);
    }

    // 특정 회원의 모든 Refresh Token 삭제
    public void deleteAllByMemberId(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    // 예측하기 어려운 랜덤 Refresh Token 원문 생성
    private String generateRawToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTE_SIZE];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // DB에는 Refresh Token 원문이 아닌 해시값만 저장
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }
}