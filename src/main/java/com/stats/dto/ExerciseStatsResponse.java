package com.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder
@AllArgsConstructor
public class ExerciseStatsResponse {
    // ?곗냽?쇰줈 ?대룞???쇱닔
    private int streak;

    // ?붿껌 湲곌컙 ?덉뿉???꾨즺???대룞 ?몄뀡 ??
    private int sessions;

    private int completedPoses;

    private int bestDailyPoses;

    // ?붿껌 湲곌컙 ?덉쓽 珥??대룞 ?쒓컙(遺?
    private int minutes;

    // ?쒖옉???몄뀡 以??꾨즺???몄뀡 鍮꾩쑉(%)
    private int completion;

    // ?ㅽ듃?덉묶/?붽? ?몄뀡 ??
    // ?꾨줎??EXERCISE_DATA.type 援ъ“? 留욎텣 ?꾨뱶紐?
    private ExerciseTypeResponse type;

    // ?대룞 罹섎┛???쒖떆 ?곗씠??
    // ?꾨줎??EXERCISE_DATA.cal 援ъ“? 留욎텣 ?꾨뱶紐?
    private List<ExerciseCalendarResponse> cal;

    // ?먯꽭蹂?肄붿묶 ?잛닔 ?먮뒗 ?됯퇏 肄붿묶 ?잛닔
    private List<ExerciseCoachingResponse> coaching;

    // 理쒓렐 ?대룞 ?몄뀡 濡쒓렇
    private List<ExerciseLogResponse> logs;
}

