package com.stats.control;

import com.security.dto.ApiResponse;
import com.stats.dto.ExerciseStatsResponse;
import com.stats.service.ExerciseStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class ExerciseStatsController {

    private final ExerciseStatsService exerciseStatsService;


    @GetMapping("/exercise")
    public ApiResponse<ExerciseStatsResponse> getExerciseStats(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String period
    ) {
        try {

            return ApiResponse.success(
                    exerciseStatsService.getExerciseStats(period, memberId)
            );
        } catch (IllegalArgumentException e) {

            return ApiResponse.error(e.getMessage());
        }
    }
}
