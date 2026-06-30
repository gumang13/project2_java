package com.security.config;

import com.security.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// fastapi 요청 인증
/*
FastAPI 요청
→ InternalApiAuthenticationFilter에서 X-Internal-Secret 검사
→ 맞으면 통과
→ SecurityConfig에서 해당 경로 permitAll
→ Controller 도달
 */
import com.security.filter.InternalApiAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiAuthenticationFilter internalApiAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/ping",
                                "/api/members/signup",
                                "/api/v1/analyze", // 외부 개발자 API : JWT 면제, 컨트롤러에서 API 키로 인증
                                "/api/settings/calibration/sessions/result", // Jwt는 면제지만 x-internal-secret 검사
                                "/api/settings/calibration/analysis/**",
                                "/api/analysis-sessions/end",
                                "/api/analysis-sessions/cleanup-unfinished",
                                "/api/analysis-sessions/events"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex  // JWT 미인증 요청에 401을 반환(기존 403 반환)
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                )
                )                
                .addFilterBefore(internalApiAuthenticationFilter, // fastapi 인증
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 로그인 검증용
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}