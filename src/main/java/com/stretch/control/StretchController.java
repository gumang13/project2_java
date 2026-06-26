package com.stretch.control;

import com.stretch.dto.StretchResponse;
import com.stretch.service.StretchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stretch")
public class StretchController {

    private final StretchService stretchService;

    @GetMapping("/page")
    public StretchResponse getStretchPage(@AuthenticationPrincipal Long memberId) {
        return stretchService.getStretchPage(memberId);
    }
}
