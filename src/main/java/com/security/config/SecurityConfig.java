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
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.*;

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
                .cors(Customizer.withDefaults())// ← 추가 (CORS 활성화)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/ping",
                                "/api/members/signup",
                                "/api/v1/**", // 외부 API, 키 인증은 컨트롤러에서
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
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(java.util.List.of("*"));   // 데모용. 운영 땐 콘솔 주소로 좁히기
        c.setAllowedMethods(java.util.List.of("*"));
        c.setAllowedHeaders(java.util.List.of("*"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }

}