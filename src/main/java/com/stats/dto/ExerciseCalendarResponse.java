package com.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExerciseCalendarResponse {

    // 프론트 캘린더 셀이 d
    private String d;

    // 운동량 단계
    // 0: 운동 없음, 1: 적음, 2: 보통, 3: 많음
    private int lv;

    // 오늘 날짜인지 여부
    private boolean today;
}
