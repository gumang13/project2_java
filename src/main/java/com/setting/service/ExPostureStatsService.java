package com.setting.service;

import com.setting.dto.ExPostureBenchmarkResponse;
import com.setting.dto.ExPostureCoachingResponse;
import com.setting.dto.ExPostureDailyResponse;
import com.stats.entity.DailyStats;
import com.stats.repository.DailyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 외부 통계 API 집계 로직. daily_stats 만으로 "나(키 소유자) vs 익명 모집단"을 계산한다.
 * 개인 식별 정보는 응답에 담지 않고, 호출자 본인 데이터만 개인 수준으로 노출한다.
 */
@Service
@RequiredArgsConstructor
public class ExPostureStatsService {

    private final DailyStatsRepository dailyStatsRepository;

    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_PER_PAGE = 100;

    // ───────────── 공개 메서드 ─────────────

    // ① 나 vs 전체 평균
    public ExPostureBenchmarkResponse benchmark(Long memberId, LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        List<DailyStats> all = dailyStatsRepository.findByStatDateBetween(startOf(range[0]), endOf(range[1]));

        Map<Long, long[]> sums = memberSums(all);      // memberId -> [goodSec, totalSec]
        List<Double> ratios = memberRatios(sums);      // 회원별 좋은 자세율(%)
        double popAvg = average(ratios);

        long[] mine = sums.get(memberId);
        boolean hasData = mine != null && mine[1] > 0;
        double myRatio = hasData ? round1(mine[0] * 100.0 / mine[1]) : 0.0;

        int percentile = 0;
        if (hasData && !ratios.isEmpty()) {
            long below = ratios.stream().filter(v -> v < myRatio).count();
            percentile = (int) Math.round(below * 100.0 / ratios.size());
        }

        return new ExPostureBenchmarkResponse(
                range[0], range[1], hasData, myRatio, popAvg,
                round1(myRatio - popAvg), percentile, ratios.size());
    }

    // ② 나의 일자별 추이
    public ExPostureDailyResponse daily(Long memberId, LocalDate from, LocalDate to, int page, int perPage) {
        LocalDate[] range = resolveRange(from, to);
        int safePage = Math.max(page, 1);
        int safePerPage = Math.min(Math.max(perPage, 1), MAX_PER_PAGE);

        List<DailyStats> mineRows = dailyStatsRepository.findByMemberIdAndStatDateBetween(
                memberId, startOf(range[0]), endOf(range[1]));

        // 날짜별 집계 (같은 날 여러 행 방어), 최신순 정렬
        Map<LocalDate, long[]> byDate = new TreeMap<>(Comparator.reverseOrder());
        for (DailyStats d : mineRows) {
            long[] a = byDate.computeIfAbsent(d.getStatDate().toLocalDate(), k -> new long[3]); // good,total,noti
            a[0] += d.getGoodPostureSec();
            a[1] += d.getTotalMeasureSec();
            a[2] += d.getNotiCount();
        }
        List<ExPostureDailyResponse.Day> allDays = byDate.entrySet().stream()
                .map(e -> new ExPostureDailyResponse.Day(
                        e.getKey(),
                        ratioPct(e.getValue()[0], e.getValue()[1]),
                        (int) (e.getValue()[1] / 60),
                        (int) e.getValue()[2]))
                .collect(Collectors.toList());

        long total = allDays.size();
        int fromIdx = Math.min((safePage - 1) * safePerPage, allDays.size());
        int toIdx = Math.min(fromIdx + safePerPage, allDays.size());
        List<ExPostureDailyResponse.Day> pageItems = new ArrayList<>(allDays.subList(fromIdx, toIdx));

        double popAvg = populationAvg(range[0], range[1]);

        return new ExPostureDailyResponse(
                safePage, safePerPage, total, pageItems.size(), popAvg, pageItems);
    }

    // ③ 코칭 판정
    public ExPostureCoachingResponse coaching(Long memberId, LocalDate from, LocalDate to, int notiThreshold) {
        LocalDate[] range = resolveRange(from, to);
        List<DailyStats> mineRows = dailyStatsRepository.findByMemberIdAndStatDateBetween(
                memberId, startOf(range[0]), endOf(range[1]));

        long good = 0, total = 0, noti = 0;
        Set<LocalDate> days = new HashSet<>();
        for (DailyStats d : mineRows) {
            good += d.getGoodPostureSec();
            total += d.getTotalMeasureSec();
            noti += d.getNotiCount();
            days.add(d.getStatDate().toLocalDate());
        }
        double myRatio = ratioPct(good, total);
        double myAvgNoti = days.isEmpty() ? 0.0 : round1((double) noti / days.size());
        double popAvg = populationAvg(range[0], range[1]);

        List<String> reasons = new ArrayList<>();
        boolean belowAvg = total > 0 && myRatio < popAvg;
        boolean overNoti = myAvgNoti >= notiThreshold;
        if (belowAvg) reasons.add(String.format("좋은 자세율 %.1f%% < 전체 평균 %.1f%%", myRatio, popAvg));
        if (overNoti) reasons.add(String.format("일 평균 알림 %.1f회 ≥ 임계값 %d회", myAvgNoti, notiThreshold));

        boolean need = belowAvg || overNoti;
        if (reasons.isEmpty()) reasons.add("자세율·알림 모두 정상 범위입니다.");
        String rec = need
                ? "지금 2~3분 목·어깨 스트레칭과 휴식을 권장합니다."
                : "좋은 자세를 잘 유지하고 있어요. 지금 페이스를 유지하세요.";

        return new ExPostureCoachingResponse(
                need, myRatio, popAvg, myAvgNoti, notiThreshold, reasons, rec);
    }

    // ───────────── 내부 유틸 ─────────────

    private double populationAvg(LocalDate from, LocalDate to) {
        List<DailyStats> all = dailyStatsRepository.findByStatDateBetween(startOf(from), endOf(to));
        return average(memberRatios(memberSums(all)));
    }

    // memberId -> [goodSec 합, totalSec 합]
    private Map<Long, long[]> memberSums(List<DailyStats> rows) {
        Map<Long, long[]> m = new HashMap<>();
        for (DailyStats d : rows) {
            long[] a = m.computeIfAbsent(d.getMemberId(), k -> new long[2]);
            a[0] += d.getGoodPostureSec();
            a[1] += d.getTotalMeasureSec();
        }
        return m;
    }

    // 회원별 좋은 자세율(%) 목록 (측정 시간이 있는 회원만)
    private List<Double> memberRatios(Map<Long, long[]> sums) {
        List<Double> ratios = new ArrayList<>();
        for (long[] a : sums.values()) {
            if (a[1] > 0) ratios.add(a[0] * 100.0 / a[1]);
        }
        return ratios;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return round1(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
        return new LocalDate[]{start, end};
    }

    private LocalDateTime startOf(LocalDate d) { return d.atStartOfDay(); }

    private LocalDateTime endOf(LocalDate d) { return d.atTime(LocalTime.MAX); }

    private double ratioPct(long good, long total) {
        if (total <= 0) return 0.0;
        return round1(good * 100.0 / total);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
