/*

   fastapi에서 보내는 요청에 대해서 인증하기 위한 클래스

 */

package com.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.springframework.stereotype.Component;

@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    private final String internalApiSecret;

    public InternalApiAuthenticationFilter(
            @Value("${internal.api.secret}") String internalApiSecret
    ) {
        this.internalApiSecret = internalApiSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        boolean internalApiRequest =
                path.equals("/api/settings/calibration/sessions/result")  // 캘리브레이션 시작
                        || path.startsWith("/api/settings/calibration/analysis/") // 캘리브레이션 주기
                        || path.equals("/api/analysis-sessions/end")  // 분석 세션 끝
                        || path.equals("/api/analysis-sessions/cleanup-unfinished") // 종료 안 된 분석 세션 정리
                        || path.equals("/api/analysis-sessions/events"); // 이벤트 발생

        if (!internalApiRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestSecret = request.getHeader("X-Internal-Secret");

        if (!internalApiSecret.equals(requestSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}