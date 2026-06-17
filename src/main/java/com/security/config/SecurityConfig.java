package com.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // 여기서 어떤 주소는 허용하고, 어떤 주소는 로그인이 필요한지
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/", "/index.html","/api/ping").permitAll()
                        .requestMatchers("/secure.html").authenticated()
                        // 위에서 따로 지정하지 않은 나머지 요청은 일단 모두 허용
                        .anyRequest().permitAll()
                )

                // Spring Security 기본 로그인 폼 사용
                .formLogin(form -> form

                        // 로그인 성공 후 이동할 주소  true를 주면 이전 요청과 상관없이 무조건 secure.html로 이동
                        .defaultSuccessUrl("/secure.html", true)
                        // 로그인 페이지와 로그인 처리 요청은 누구나 접근 가능하게 허용
                        .permitAll()
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        // 위에서 설정한 내용을 기반으로 SecurityFilterChain 생성
        return http.build();
    }

}