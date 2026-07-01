package com.stretch.control;

import com.stretch.dto.PresetResponse;
import com.stretch.dto.RoutineSaveRequest;
import com.stretch.dto.StretchPoseCompleteRequest;
import com.stretch.dto.StretchPoseCompleteResponse;
import com.stretch.dto.StretchResponse;
import com.stretch.dto.StretchSessionStartRequest;
import com.stretch.dto.StretchSessionStartResponse;
import com.stretch.service.StretchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stretch")
public class StretchController {

    private final StretchService stretchService;

    @GetMapping("/page")
    public StretchResponse getStretchPage(@AuthenticationPrincipal Long memberId) {
        return stretchService.getStretchPage(memberId);
    }

    @GetMapping("/routines")
    public List<PresetResponse> getMyRoutines(@AuthenticationPrincipal Long memberId) {
        return stretchService.getMyRoutines(memberId);
    }

    @PostMapping("/routines")
    public PresetResponse saveMyRoutine(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RoutineSaveRequest request
    ) {
        return stretchService.saveMyRoutine(memberId, request);
    }

    @PostMapping("/sessions")
    public StretchSessionStartResponse startSession(
            @AuthenticationPrincipal Long memberId,
            @RequestBody StretchSessionStartRequest request
    ) {
        return stretchService.startSession(memberId, request);
    }

    @PostMapping("/sessions/{sessionId}/poses")
    public StretchPoseCompleteResponse completePose(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long sessionId,
            @RequestBody StretchPoseCompleteRequest request
    ) {
        return stretchService.completePose(memberId, sessionId, request);
    }
}