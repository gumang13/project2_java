package com.stats.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogDto {
    private String type;   // "alert" / "recover"
    private String text;   // "거북목 자세 감지" / "바른 자세로 복귀"
    private String dur;    // "4분 23초" / "복귀까지 4분 23초"
    private String time;   // "11:42"
}
