package com.stats.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class MonitorStatsDto {

    // ===== 요약 카드 영역 (상단 4개 지표) =====

    private int ratio;            // 바른자세 비율(%). goodMinutes / 전체측정시간 * 100
    // 예: 153분 중 106분이 바른자세면 69

    private int ratioTrend;       // 비율 증감(전날/지난주 대비). 예: +5 = 5%p 올라감
    // 1차 구현에선 0으로 둠 (직전 기간 따로 계산해야 해서 보류)

    private int alerts;           // 거북목 알림 횟수. BAD_POSTURE 이벤트 개수
    // 예: 오늘 3번 거북목 진입했으면 3

    private int alertsTrend;      // 알림 횟수 증감. 예: -1 = 어제보다 1번 덜 울림
    // 1차 구현에선 0 (보류)

    private int badMinutes;       // 거북목 누적 시간(분). 진입~회복 쌍의 지속시간 합 / 60
    // 예: 거북목 상태로 총 47분 있었으면 47

    private double badMinutesTrend; // 거북목 시간 증감(분). 예: -0.3 = 0.3분 줄어듦
    // 1차 구현에선 0 (보류)

    private int goodMinutes;      // 바른자세 누적 시간(분). (전체측정시간 - 거북목시간) / 60
    // 예: 전체 153분 중 거북목 47분이면 106

    // ===== 차트 영역 =====

    private List<String> labels;  // x축 라벨. 기간에 따라 모양이 다름
    // day:   ['9시','11시','13시', ...]
    // week:  ['월','화','수','목','금','토','일']
    // month: ['1주','2주','3주','4주']

    private List<Double> goodHours;  // 라벨별 바른자세 시간(시간 단위)
    // RatioLineChart, StackBarChart에서 씀
    // 예: [0.7, 0.5, 0.3, ...] (9시엔 0.7시간 바른자세)

    private List<Double> totalHours; // 라벨별 전체 측정 시간(시간 단위)
    // goodHours와 함께 비율/스택 계산에 쓰임
    // 예: [1.0, 0.9, 0.8, ...] (9시엔 1.0시간 측정)

    private List<Integer> hourly;    // 시간대별 거북목 발생 횟수. HourBarChart에서 씀
    // day는 9~18시 10칸, BAD_POSTURE를 시각별로 카운트
    // 예: [0,0,1,0,1,1,1,0,0,0]

    // ===== 로그 영역 (하단 '오늘 이벤트 로그') =====

    private List<LogDto> logs;    // 개별 이벤트 목록. 이벤트 한 건 = LogDto 한 개
    // 최근 N건만 잘라서 내려줄 예정
}
