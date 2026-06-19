package com.security.control;

import com.member.dto.MemberDto;
import com.member.repository.MemberRepository;
import com.security.dto.ApiResponse;
import com.security.dto.TokenResponse;
import com.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberRepository memberRepository;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody MemberDto req) {
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
        return ApiResponse.success(new TokenResponse(token));
    }
}