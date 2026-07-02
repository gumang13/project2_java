package com.security.control;

import com.member.dto.MemberDto;
import com.member.repository.MemberRepository;
import com.security.dto.ApiResponse;
import com.security.dto.TokenResponse;
import com.security.entity.RefreshToken;
import com.security.jwt.JwtTokenProvider;
import com.security.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody MemberDto req, HttpServletResponse response ) {
        try {
            // 아이디/비번 검증 (틀리면 예외 발생)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException e) {
            return ApiResponse.error("아이디 또는 비밀번호가 틀렸습니다");
        }

        Long userId = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다.")).getId();

        // 통과하면 토큰 발급
        String token = tokenProvider.createToken(userId, req.getEmail());

        // 로그인 시 기존 Refresh Token을 모두 삭제하여 한 계정당 하나의 세션만 유지
        refreshTokenService.deleteAllByMemberId(userId);

        // Access Token 재발급에 사용할 Refresh Token 발급
        String refreshToken = refreshTokenService.issue(userId);

        // Refresh Token은 JavaScript에서 읽을 수 없도록 HttpOnly Cookie에 저장
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // 로컬 개발 환경은 false, HTTPS 배포 환경은 true 권장
                .path("/api/auth")
                .maxAge(Duration.ofHours(12))
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ApiResponse.success(new TokenResponse(token));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 브라우저가 HttpOnly refreshToken 쿠키를 자동으로 함께 보냄
        // JavaScript는 쿠키 값을 읽지 못하지만, 요청에는 자동 첨부됨
        String rawRefreshToken = getRefreshTokenFromCookie(request);

        if (rawRefreshToken == null) {
            return ApiResponse.error("Refresh Token이 없습니다.");
        }

        try {
            // DB에 저장된 해시와 만료 시간, 폐기 여부를 검증
            RefreshToken savedRefreshToken = refreshTokenService.validate(rawRefreshToken);

            // 새 Access Token을 만들기 위해 회원 이메일 조회
            String email = memberRepository.findById(savedRefreshToken.getMemberId())
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."))
                    .getEmail();

            // Refresh Token rotation: 기존 토큰은 폐기하고 새 토큰 발급
            String newRefreshToken = refreshTokenService.rotate(rawRefreshToken);

            // API Authorization 헤더에 다시 넣을 새 Access Token 발급
            String newAccessToken = tokenProvider.createToken(savedRefreshToken.getMemberId(), email);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/api/auth")
                    .maxAge(Duration.ofHours(12))
                    .sameSite("Lax")
                    .build();

            response.addHeader("Set-Cookie", refreshCookie.toString());

            return ApiResponse.success(new TokenResponse(newAccessToken));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Refresh Token이 유효하지 않습니다.");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 현재 요청에 포함된 Refresh Token 쿠키 조회
        String rawRefreshToken = getRefreshTokenFromCookie(request);

        // DB에 저장된 Refresh Token을 폐기 처리
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }

        // 브라우저에 저장된 HttpOnly refreshToken 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());

        return ApiResponse.success(null);
    }

    // 브라우저가 보낸 쿠키 목록에서 refreshToken 값을 꺼냄
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}