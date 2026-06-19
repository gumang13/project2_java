package com.security.control;

import com.security.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public ApiResponse<String> me(@AuthenticationPrincipal String username) {
        return ApiResponse.success("로그인된 사용자: " + username);
    }
}