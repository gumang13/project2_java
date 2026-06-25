package com.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder
@AllArgsConstructor
public class ExerciseStatsResponse {
    // 연속으로 운동한 일수
    private int streak;

    // 요청 기간 안에서 완료한 운동 세션 수
    private int sessions;

    // 요청 기간 안의 총 운동 시간(분)
    private int minutes;

    // 시작한 세션 중 완료한 세션 비율(%)
    private int completion;

    // 스트레칭/요가 세션 수
    // 프론트 EXERCISE_DATA.type 구조와 맞춘 필드명
    private ExerciseTypeResponse type;

    // 운동 캘린더 표시 데이터
    // 프론트 EXERCISE_DATA.cal 구조와 맞춘 필드명
    private List<ExerciseCalendarResponse> cal;

    // 자세별 코칭 횟수 또는 평균 코칭 횟수
    private List<ExerciseCoachingResponse> coaching;

    // 최근 운동 세션 로그
    private List<ExerciseLogResponse> logs;
}
