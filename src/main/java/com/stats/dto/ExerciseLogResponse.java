package com.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExerciseLogResponse {

    // 운동 종류
    private String type;

    // 루틴명
    private String name;

    // 화면 표시용 수행 시각

    private String time;

    // 운동 소요 시간
    // 프론트 로그 mock의 dur 필드
    private String dur;

    // 완료 자세 수
    // 프론트 로그 mock의 done 필드
    private String done;

    // 해당 세션에서 받은 총 코칭 횟수
    private int coach;
}
