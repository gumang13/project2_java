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
            @RequestBody MemberDto req,
            HttpServletResponse response
    ) {
        try {
            // ?꾩씠??鍮꾨쾲 寃利?(?由щ㈃ ?덉쇅 諛쒖깮)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException e) {
            return ApiResponse.error("?꾩씠???먮뒗 鍮꾨?踰덊샇媛 ??몄뒿?덈떎");
        }
        Long userId = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("?뚯썝 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.")).getId();

        // ?듦낵?섎㈃ ?좏겙 諛쒓툒
        String token = tokenProvider.createToken(userId, req.getEmail());

        // 로그인 시 기존 Refresh Token을 모두 삭제하여 한 계정당 하나의 세션만 유지
        refreshTokenService.deleteAllByMemberId(userId);

        // Access Token ?щ컻湲됱뿉 ?ъ슜??Refresh Token 諛쒓툒
        String refreshToken = refreshTokenService.issue(userId);

        // Refresh Token? JavaScript?먯꽌 ?쎌쓣 ???녿룄濡?HttpOnly Cookie?????
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // 濡쒖뺄 媛쒕컻 ?섍꼍?먯꽌??false, HTTPS 諛고룷 ?섍꼍?먯꽌??true 沅뚯옣
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
        // 釉뚮씪?곗?媛 HttpOnly refreshToken 荑좏궎瑜??먮룞?쇰줈 ?④퍡 蹂대깂
        // JavaScript?먯꽌?????좏겙 媛믪쓣 吏곸젒 ?쎌쓣 ???놁쓬
        String rawRefreshToken = getRefreshTokenFromCookie(request);

        if (rawRefreshToken == null) {
            return ApiResponse.error("Refresh Token???놁뒿?덈떎.");
        }

        try {
            // DB????λ맂 ?댁떆媛? 留뚮즺 ?쒓컙, ?먭린 ?щ?瑜?寃利?            RefreshToken savedRefreshToken = refreshTokenService.validate(rawRefreshToken);

            // 濡쒓렇????諛쒓툒??Access Token泥섎읆 subject???대찓?쇱쓣 ?ｊ린 ?꾪빐 議고쉶
            RefreshToken savedRefreshToken = refreshTokenService.validate(rawRefreshToken);

            String email = memberRepository.findById(savedRefreshToken.getMemberId())
                    .orElseThrow(() -> new RuntimeException("?뚯썝 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎."))
                    .getEmail();

            // Refresh Token rotation: 湲곗〈 ?좏겙???먭린?섍퀬 ???좏겙 諛쒓툒
            String newRefreshToken = refreshTokenService.rotate(rawRefreshToken);

            // API Authorization ?ㅻ뜑???ъ슜????Access Token 諛쒓툒
            String newAccessToken = tokenProvider.createToken(savedRefreshToken.getMemberId(), email);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken).httpOnly(true)
                    .secure(false)
                    .path("/api/auth")
                    .maxAge(Duration.ofHours(12))
                    .sameSite("Lax")
                    .build();

            response.addHeader("Set-Cookie", refreshCookie.toString());

            return ApiResponse.success(new TokenResponse(newAccessToken));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Refresh Token???좏슚?섏? ?딆뒿?덈떎.");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // ?꾩옱 釉뚮씪?곗?媛 蹂대궦 Refresh Token 荑좏궎瑜??뺤씤
        String rawRefreshToken = getRefreshTokenFromCookie(request);

        // DB????λ맂 Refresh Token???먭린 泥섎━
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }

        // 釉뚮씪?곗?????λ맂 HttpOnly refreshToken 荑좏궎 ??젣
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

    // 釉뚮씪?곗?媛 蹂대궦 荑좏궎 紐⑸줉?먯꽌 refreshToken 媛믪쓣 爰쇰깂
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
