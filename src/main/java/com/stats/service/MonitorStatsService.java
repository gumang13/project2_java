package com.stats.service;

import com.analysis.constant.EventType;
import com.analysis.entity.Analysis;
import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;
import com.analysis.repository.AnalysisRepository;
import com.stats.dto.LogDto;
import com.stats.dto.MonitorStatsDto;
import com.stats.entity.DailyStats;
import com.stats.repository.DailyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorStatsService {

    private final AnalysisRepository analysisRepository;
    private final AnalysisEventRepository analysisEventRepository;
    private final DailyStatsRepository dailyStatsRepository;

    public MonitorStatsDto getMonitorStats(String period,Long id) {
        MonitorStatsDto monitorStatsDto = new MonitorStatsDto();

        // react에 day, week , month 로 되어있어서 react 기준으로 수정
        // day는 오늘 실시간 데이터라 analysis + analysis_events 원본으로 계산
        if (period.equals("day")) {
            LocalDateTime from = getPeriodStart(period);
            LocalDateTime to = getPeriodEnd(period);

            List<Analysis> analyses = analysisRepository.findByMemberIdAndPeriodOverlap(id, from, to);
            if (analyses.isEmpty()) {
                return createEmptyStats(period);
            }

            List<Long> analysisIds = analyses.stream()
                    .map(Analysis::getId)
                    .toList();

            List<AnalysisEvent> events = analysisEventRepository.findByAnalysisIdIn(analysisIds);

            return buildDayStats(events, analyses, from, to);
        }

// week/month는 daily_stats에 저장된 일별 집계값으로 계산합니다.
        LocalDate fromDate = switch (period) {
            case "week" -> LocalDate.now().minusDays(6);
            case "month" -> LocalDate.now().minusDays(29);
            default -> throw new IllegalArgumentException("period 가 일치하지 않습니다");
        };

        LocalDate toDate = LocalDate.now();
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();

        List<DailyStats> dailyStats =
                dailyStatsRepository.findByMemberIdAndStatDateBetween(
                        id,
                        fromDateTime,
                        toDateTime
                );

        if (dailyStats.isEmpty()) {
            return createEmptyStats(period);
        }

        List<Analysis> analyses = analysisRepository.findByMemberIdAndPeriodOverlap(id, fromDateTime, toDateTime);
        List<Long> analysisIds = analyses.stream()
                .map(Analysis::getId)
                .toList();
        List<AnalysisEvent> events = analysisIds.isEmpty()
                ? List.of()
                : analysisEventRepository.findByAnalysisIdIn(analysisIds);

        return buildRangeStats(period, dailyStats, events, fromDate, fromDateTime, toDateTime);
    }

    // 추가: 측정 데이터가 없을 때도 프론트 차트가 null 때문에 깨지지 않도록 빈 응답을 만듭니다.
    private MonitorStatsDto createEmptyStats(String period) {
        MonitorStatsDto dto = new MonitorStatsDto();
        List<TimeBucket> buckets = createEmptyBuckets(period);
        List<Long> emptySeconds = new ArrayList<>(Collections.nCopies(buckets.size(), 0L));

        dto.setRatio(0);
        dto.setRatioTrend(0);
        dto.setAlerts(0);
        dto.setAlertsTrend(0);
        dto.setBadMinutes(0);
        dto.setBadMinutesTrend(0);
        dto.setGoodMinutes(0);
        dto.setLabels(createLabels(buckets));
        dto.setGoodHours(toHours(emptySeconds));
        dto.setTotalHours(toHours(emptySeconds));
        dto.setHourly(new ArrayList<>(Collections.nCopies(buckets.size(), 0)));
        dto.setLogs(List.of());

        return dto;
    }

    // 추가: 데이터가 없는 경우에도 period별 기본 차트 구간을 제공합니다.
    private List<TimeBucket> createEmptyBuckets(String period) {
        LocalDateTime from = getPeriodStart(period);
        LocalDateTime to = getPeriodEnd(period);

        return switch (period) {
            case "day" -> List.of();
            case "week" -> createDayRangeBuckets(from.toLocalDate(), 7);
            case "month" -> createWeekRangeBuckets(from.toLocalDate(), to.toLocalDate());
            default -> List.of();
        };
    }

    // 추가: 프론트 period 값에 맞춰 조회 시작 시각을 계산합니다.
    private LocalDateTime getPeriodStart(String period) {
        LocalDate today = LocalDate.now();

        return switch (period) {
            case "day" -> today.atStartOfDay();
            case "week" -> today.minusDays(6).atStartOfDay();
            case "month" -> today.minusDays(29).atStartOfDay();
            default -> throw new IllegalArgumentException("period 가 일치하지 않습니다");
        };
    }

    // 추가: 진행 중인 측정은 현재 시각까지만 계산하도록 조회 종료 시각을 현재로 사용합니다.
    private LocalDateTime getPeriodEnd(String period) {
        if (!period.equals("day") && !period.equals("week") && !period.equals("month")) {
            throw new IllegalArgumentException("period 가 일치하지 않습니다");
        }

        return LocalDateTime.now();
    }

    // 추가: 요청 기간의 측정 세션과 자세 이벤트 목록으로 통계 값을 계산합니다.
    private MonitorStatsDto buildDayStats(
            List<AnalysisEvent> events,
            List<Analysis> analyses,
            LocalDateTime from,
            LocalDateTime to
    ) {
        MonitorStatsDto dto = new MonitorStatsDto();

        // BAD_POSTURE와 GOOD_POSTURE를 시간순으로 짝지어 계산하기 위해 정렬합니다.
        events.sort(Comparator.comparing(AnalysisEvent::getEventAt));

        int alerts = 0;
        long badSeconds = 0;
        LocalDateTime badStartedAt = null;

        

        for (AnalysisEvent event : events) {
            if (event.getEventType() == EventType.BAD_POSTURE) {
                if (isInPeriod(event.getEventAt(), from, to)) {
                    alerts++;
                }

                if (badStartedAt == null) {
                    badStartedAt = event.getEventAt();
                }
            }

            if (event.getEventType() == EventType.GOOD_POSTURE && badStartedAt != null) {
                badSeconds += calculateClippedSeconds(badStartedAt, event.getEventAt(), from, to);
                badStartedAt = null;
            }
        }

        // 아직 GOOD_POSTURE가 없다면 현재 시간까지 거북목 상태로 계산합니다.
        if (badStartedAt != null) {
            badSeconds += calculateClippedSeconds(badStartedAt, to, from, to);
        }

        // 추가: 바른 자세 비율 계산의 기준 시간을 실제 측정 세션 시간 합계로 계산합니다.
        long totalSeconds = calculateTotalMeasureSeconds(analyses, from, to);
        long goodSeconds = Math.max(0, totalSeconds - badSeconds);

        dto.setAlerts(alerts);
        dto.setAlertsTrend(0);
        dto.setBadMinutes((int) (badSeconds / 60));
        dto.setBadMinutesTrend(0);
        dto.setGoodMinutes((int) (goodSeconds / 60));
        dto.setRatio(totalSeconds == 0 ? 0 : (int) Math.round(goodSeconds * 100.0 / totalSeconds));
        dto.setRatioTrend(0);

        // 추가: 차트/로그 데이터도 요청 기간의 측정 시작~종료 시간 기준으로 채웁니다.
        List<TimeBucket> buckets = createDayBuckets(analyses, from, to);
        List<Long> totalSecondsByBucket = calculateTotalSecondsByBucket(analyses, buckets, to);
        List<Long> badSecondsByBucket = calculateBadSecondsByBucket(events, buckets, to);
        dto.setLabels(createLabels(buckets));
        dto.setTotalHours(toHours(totalSecondsByBucket));
        dto.setGoodHours(calculateGoodHours(totalSecondsByBucket, badSecondsByBucket));
        // 프론트가 hourly를 9시~18시 10칸으로 해석하므로 같은 형식으로 내려줍니다.
        dto.setHourly(calculateHourlyPattern(events, from, to));
        dto.setLogs(createLogs(events, from, to, "day"));

        return dto;
    }
    // week/month는 daily_stats에 저장된 일별 집계값으로 계산합니다.
    private MonitorStatsDto buildRangeStats(
            String period,
            List<DailyStats> dailyStats,
            List<AnalysisEvent> events,
            LocalDate fromDate,
            LocalDateTime from,
            LocalDateTime to
    ) {
        MonitorStatsDto dto = new MonitorStatsDto();
        int bucketCount = period.equals("week") ? 7 : 5;

        List<String> labels = createRangeLabels(period, bucketCount, fromDate);
        List<Integer> alertsByBucket = new ArrayList<>(Collections.nCopies(bucketCount, 0));
        List<Long> goodSecondsByBucket = new ArrayList<>(Collections.nCopies(bucketCount, 0L));
        List<Long> badSecondsByBucket = new ArrayList<>(Collections.nCopies(bucketCount, 0L));
        List<Long> totalSecondsByBucket = new ArrayList<>(Collections.nCopies(bucketCount, 0L));

        int alerts = 0;
        long goodSeconds = 0;
        long badSeconds = 0;
        long totalSeconds = 0;

        for (DailyStats dailyStat : dailyStats) {
            if (dailyStat.getStatDate() == null) {
                continue;
            }

            int index = getRangeBucketIndex(period, fromDate, dailyStat.getStatDate().toLocalDate(), bucketCount);
            if (index < 0) {
                continue;
            }

            alerts += dailyStat.getNotiCount();
            goodSeconds += dailyStat.getGoodPostureSec();
            badSeconds += dailyStat.getBadPostureSec();
            totalSeconds += dailyStat.getTotalMeasureSec();

            alertsByBucket.set(index, alertsByBucket.get(index) + dailyStat.getNotiCount());
            goodSecondsByBucket.set(index, goodSecondsByBucket.get(index) + dailyStat.getGoodPostureSec());
            badSecondsByBucket.set(index, badSecondsByBucket.get(index) + dailyStat.getBadPostureSec());
            totalSecondsByBucket.set(index, totalSecondsByBucket.get(index) + dailyStat.getTotalMeasureSec());
        }

        dto.setAlerts(alerts);
        dto.setAlertsTrend(0);
        dto.setBadMinutes((int) (badSeconds / 60));
        dto.setBadMinutesTrend(0);
        dto.setGoodMinutes((int) (goodSeconds / 60));
        dto.setRatio(totalSeconds == 0 ? 0 : (int) Math.round(goodSeconds * 100.0 / totalSeconds));
        dto.setRatioTrend(0);

        List<String> visibleLabels = new ArrayList<>();
        List<Long> visibleGoodSeconds = new ArrayList<>();
        List<Long> visibleTotalSeconds = new ArrayList<>();
        for (int i = 0; i < totalSecondsByBucket.size(); i++) {
            if (totalSecondsByBucket.get(i) <= 0) {
                continue;
            }

            visibleLabels.add(labels.get(i));
            visibleGoodSeconds.add(goodSecondsByBucket.get(i));
            visibleTotalSeconds.add(totalSecondsByBucket.get(i));
        }

        dto.setLabels(visibleLabels);
        dto.setGoodHours(toHours(visibleGoodSeconds));
        dto.setTotalHours(toHours(visibleTotalSeconds));
        // 프론트가 hourly를 9시~18시 10칸으로 해석하므로 같은 형식으로 내려줍니다.
        dto.setHourly(calculateHourlyPattern(events, from, to));
        dto.setLogs(createLogs(events, from, to, period));

        return dto;
    }
    private List<String> createRangeLabels(String period, int bucketCount, LocalDate fromDate) {
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < bucketCount; i++) {
            if (period.equals("week")) {
                labels.add(toKoreanDayOfWeek(fromDate.plusDays(i)));
            } else {
                labels.add((i + 1) + "주");
            }
        }

        return labels;
    }

    private int getRangeBucketIndex(String period, LocalDate fromDate, LocalDate statDate, int bucketCount) {
        long days = ChronoUnit.DAYS.between(fromDate, statDate);

        if (days < 0) {
            return -1;
        }

        int index = period.equals("week") ? (int) days : (int) (days / 7);
        return index >= bucketCount ? -1 : index;
    }



    // 추가: 요청 기간 안에서 진행된 측정들의 시작~종료 시간을 합산합니다. 진행 중인 측정은 현재 시간까지 계산합니다.
    private long calculateTotalMeasureSeconds(List<Analysis> analyses, LocalDateTime from, LocalDateTime to) {
        long totalSeconds = 0;

        for (Analysis analysis : analyses) {
            LocalDateTime startedAt = analysis.getStartedAt();
            if (startedAt == null) {
                continue;
            }

            LocalDateTime endedAt = analysis.getEndedAt() == null ? to : analysis.getEndedAt();
            LocalDateTime start = startedAt.isAfter(from) ? startedAt : from;
            LocalDateTime end = endedAt.isBefore(to) ? endedAt : to;

            if (end.isAfter(start)) {
                totalSeconds += ChronoUnit.SECONDS.between(start, end);
            }
        }

        return totalSeconds;
    }

    // 추가: 이벤트 시각이 요청 기간 안에 있는지 확인합니다.
    private boolean isInPeriod(LocalDateTime eventAt, LocalDateTime from, LocalDateTime to) {
        return !eventAt.isBefore(from) && eventAt.isBefore(to);
    }

    // 추가: 기간 경계에 걸친 자세 지속 시간을 요청 기간 안쪽만 잘라 계산합니다.
    private long calculateClippedSeconds(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime from,
            LocalDateTime to
    ) {
        LocalDateTime start = startedAt.isAfter(from) ? startedAt : from;
        LocalDateTime end = endedAt.isBefore(to) ? endedAt : to;

        return end.isAfter(start) ? ChronoUnit.SECONDS.between(start, end) : 0;
    }

    // 추가: 오늘 통계는 실제 측정이 있었던 시간대만 차트 구간으로 만듭니다.
    private List<TimeBucket> createDayBuckets(List<Analysis> analyses, LocalDateTime from, LocalDateTime to) {
        List<LocalDateTime> bucketStarts = new ArrayList<>();

        for (Analysis analysis : analyses) {
            LocalDateTime startedAt = analysis.getStartedAt();
            if (startedAt == null) {
                continue;
            }

            LocalDateTime endedAt = analysis.getEndedAt() == null ? to : analysis.getEndedAt();
            LocalDateTime start = startedAt.isAfter(from) ? startedAt : from;
            LocalDateTime end = endedAt.isBefore(to) ? endedAt : to;
            if (!end.isAfter(start)) {
                continue;
            }

            LocalDateTime cursor = start.truncatedTo(ChronoUnit.HOURS);
            LocalDateTime lastBucketStart = end.truncatedTo(ChronoUnit.HOURS);

            while (!cursor.isAfter(lastBucketStart)) {
                if (!bucketStarts.contains(cursor)) {
                    bucketStarts.add(cursor);
                }
                cursor = cursor.plusHours(1);
            }
        }

        bucketStarts.sort(Comparator.naturalOrder());

        List<TimeBucket> buckets = new ArrayList<>();
        for (LocalDateTime bucketStart : bucketStarts) {
            buckets.add(new TimeBucket(bucketStart.getHour() + "시", bucketStart, bucketStart.plusHours(1)));
        }

        return buckets;
    }

    // 추가: 주간 통계는 날짜별 차트 구간을 만듭니다.
    private List<TimeBucket> createDayRangeBuckets(LocalDate fromDate, int days) {
        List<TimeBucket> buckets = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = fromDate.plusDays(i);
            buckets.add(new TimeBucket(toKoreanDayOfWeek(date), date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
        }

        return buckets;
    }

    // 추가: 월간 통계는 7일 단위 주차 차트 구간을 만듭니다.
    private List<TimeBucket> createWeekRangeBuckets(LocalDate fromDate, LocalDate toDate) {
        List<TimeBucket> buckets = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            LocalDate startDate = fromDate.plusDays(i * 7L);
            LocalDate endDate = startDate.plusDays(7);
            if (startDate.isAfter(toDate)) {
                break;
            }

            buckets.add(new TimeBucket((i + 1) + "주", startDate.atStartOfDay(), endDate.atStartOfDay()));
        }

        return buckets;
    }

    // 추가: 측정 세션이 각 차트 구간과 겹치는 시간을 합산합니다.
    private List<Long> calculateTotalSecondsByBucket(
            List<Analysis> analyses,
            List<TimeBucket> buckets,
            LocalDateTime now
    ) {
        List<Long> secondsByBucket = new ArrayList<>(Collections.nCopies(buckets.size(), 0L));

        for (Analysis analysis : analyses) {
            LocalDateTime startedAt = analysis.getStartedAt();
            if (startedAt == null) {
                continue;
            }

            LocalDateTime endedAt = analysis.getEndedAt() == null ? now : analysis.getEndedAt();
            addSecondsByBucket(secondsByBucket, buckets, startedAt, endedAt);
        }

        return secondsByBucket;
    }

    // 추가: 거북목 상태가 각 차트 구간에 얼마나 지속됐는지 합산합니다.
    private List<Long> calculateBadSecondsByBucket(
            List<AnalysisEvent> events,
            List<TimeBucket> buckets,
            LocalDateTime now
    ) {
        List<Long> secondsByBucket = new ArrayList<>(Collections.nCopies(buckets.size(), 0L));
        LocalDateTime badStartedAt = null;

        for (AnalysisEvent event : events) {
            if (event.getEventType() == EventType.BAD_POSTURE && badStartedAt == null) {
                badStartedAt = event.getEventAt();
            }

            if (event.getEventType() == EventType.GOOD_POSTURE && badStartedAt != null) {
                addSecondsByBucket(secondsByBucket, buckets, badStartedAt, event.getEventAt());
                badStartedAt = null;
            }
        }

        if (badStartedAt != null) {
            addSecondsByBucket(secondsByBucket, buckets, badStartedAt, now);
        }

        return secondsByBucket;
    }

    // 추가: 시작~종료 시간을 차트 구간에 나눠 더합니다.
    private void addSecondsByBucket(
            List<Long> secondsByBucket,
            List<TimeBucket> buckets,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        if (!endedAt.isAfter(startedAt)) {
            return;
        }

        for (int i = 0; i < buckets.size(); i++) {
            TimeBucket bucket = buckets.get(i);
            LocalDateTime start = startedAt.isAfter(bucket.from()) ? startedAt : bucket.from();
            LocalDateTime end = endedAt.isBefore(bucket.to()) ? endedAt : bucket.to();

            if (end.isAfter(start)) {
                secondsByBucket.set(i, secondsByBucket.get(i) + ChronoUnit.SECONDS.between(start, end));
            }
        }
    }

    // 추가: 차트 x축 라벨을 차트 구간 기준으로 만듭니다.
    private List<String> createLabels(List<TimeBucket> buckets) {
        List<String> labels = new ArrayList<>();

        for (TimeBucket bucket : buckets) {
            labels.add(bucket.label());
        }

        return labels;
    }

    // 추가: 프론트 시간대 차트가 9시~18시 10칸으로 고정되어 있어, 모든 period에서 같은 형식으로 내려줍니다.
    private List<Integer> calculateHourlyPattern(List<AnalysisEvent> events, LocalDateTime from, LocalDateTime to) {
        List<Integer> hourly = new ArrayList<>(Collections.nCopies(10, 0));

        for (AnalysisEvent event : events) {
            if (event.getEventType() != EventType.BAD_POSTURE || !isInPeriod(event.getEventAt(), from, to)) {
                continue;
            }

            int hour = event.getEventAt().getHour();
            if (hour >= 9 && hour <= 18) {
                int index = hour - 9;
                hourly.set(index, hourly.get(index) + 1);
            }
        }

        return hourly;
    }

    // 추가: 초 단위 값을 프론트 차트에서 쓰는 시간 단위로 변환합니다.
    private List<Double> toHours(List<Long> secondsByHour) {
        List<Double> hours = new ArrayList<>();

        for (Long seconds : secondsByHour) {
            hours.add(roundHour(seconds));
        }

        return hours;
    }

    // 추가: 전체 측정 시간에서 거북목 시간을 뺀 바른 자세 시간을 계산합니다.
    private List<Double> calculateGoodHours(List<Long> totalSecondsByHour, List<Long> badSecondsByHour) {
        List<Double> goodHours = new ArrayList<>();

        for (int i = 0; i < totalSecondsByHour.size(); i++) {
            long seconds = Math.max(0, totalSecondsByHour.get(i) - badSecondsByHour.get(i));
            goodHours.add(roundHour(seconds));
        }

        return goodHours;
    }

    // 추가: 최근 이벤트 로그를 프론트 LogDto 형태로 변환합니다.
    private List<LogDto> createLogs(List<AnalysisEvent> events, LocalDateTime from, LocalDateTime to, String period) {
        events.sort(Comparator.comparing(AnalysisEvent::getEventAt));

        List<LogDto> logs = new ArrayList<>();
        LocalDateTime badStartedAt = null;

        for (AnalysisEvent event : events) {
            if (!isInPeriod(event.getEventAt(), from, to)) {
                continue;
            }

            LogDto log = new LogDto();
            log.setTime(formatLogTime(event.getEventAt(), period));

            if (event.getEventType() == EventType.BAD_POSTURE) {
                badStartedAt = event.getEventAt();
                log.setType("alert");
                log.setText("거북목 자세 감지");
                log.setDur("0분 0초");
            } else {
                log.setType("recover");
                log.setText("바른 자세로 복귀");
                log.setDur(badStartedAt == null ? "0분 0초" : formatDuration(badStartedAt, event.getEventAt()));
                badStartedAt = null;
            }

            logs.add(log);
        }

        int fromIndex = Math.max(0, logs.size() - 6);
        return logs.subList(fromIndex, logs.size());
    }

    // 추가: 초 단위 시간을 소수점 한 자리 시간 값으로 반올림합니다.
    private String formatLogTime(LocalDateTime eventAt, String period) {
        String time = eventAt.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString();
        if (period.equals("day")) {
            return time;
        }

        return String.format("%02d/%02d %s", eventAt.getMonthValue(), eventAt.getDayOfMonth(), time);
    }

    private double roundHour(long seconds) {
        return Math.round((seconds / 3600.0) * 10) / 10.0;
    }

    // 추가: 로그에 표시할 지속 시간을 "n분 n초" 형식으로 만듭니다.
    private String formatDuration(LocalDateTime startedAt, LocalDateTime endedAt) {
        long seconds = Math.max(0, ChronoUnit.SECONDS.between(startedAt, endedAt));
        return (seconds / 60) + "분 " + (seconds % 60) + "초";
    }

    // 추가: LocalDate의 요일을 프론트 라벨에 맞는 한글 한 글자로 변환합니다.
    private String toKoreanDayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    // 추가: 차트 한 칸의 라벨과 시간 범위를 함께 관리합니다.
    private record TimeBucket(String label, LocalDateTime from, LocalDateTime to) {
    }



}
