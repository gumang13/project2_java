package com.stats.service;

import com.stats.dto.ExerciseCalendarResponse;
import com.stats.dto.ExerciseCoachingResponse;
import com.stats.dto.ExerciseLogResponse;
import com.stats.dto.ExerciseStatsResponse;
import com.stats.dto.ExerciseTypeResponse;
import com.stats.repository.ExercisePoseResultStatsRepository;
import com.stats.repository.ExerciseStatsRepository;
import com.stretch.constant.ExerciseCategory;
import com.stretch.constant.SessionStatus;
import com.stretch.entity.ExercisePose;
import com.stretch.entity.ExerciseRoutine;
import com.stretch.entity.ExerciseSession;
import com.stretch.entity.PoseResult;
import com.stretch.repository.ExercisePoseRepository;
import com.stretch.repository.ExerciseRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseStatsService {

    private static final Set<String> YOGA_POSE_KEYS = Set.of(
            "mountain_pose",
            "warrior_ii_pose",
            "triangle_pose",
            "tree_pose",
            "eagle_pose"
    );

    private final ExerciseStatsRepository exerciseStatsRepository;
    private final ExercisePoseResultStatsRepository exercisePoseResultStatsRepository;
    private final ExerciseRoutineRepository exerciseRoutineRepository;
    private final ExercisePoseRepository exercisePoseRepository;

    @Transactional(readOnly = true)
    public ExerciseStatsResponse getExerciseStats(String period, Long memberId) {
        LocalDateTime from = getPeriodStart(period);
        LocalDateTime to = LocalDateTime.now();

        // 요청 기간 안의 회원 운동 세션을 시간순으로 조회합니다.
        List<ExerciseSession> sessions = exerciseStatsRepository
                .findByMemberIdAndStartedAtBetweenOrderByStartedAtAsc(memberId, from, to);

        // 화면의 운동 횟수, 시간, 종류 비중, 캘린더는 완료 세션 기준으로 계산합니다.
        List<ExerciseSession> completedSessions = sessions.stream()
                .filter(this::isCompleted)
                .toList();

        Map<Long, ExerciseRoutine> routineMap = getRoutineMap(sessions);
        List<PoseResult> poseResults = getPoseResults(completedSessions);
        Map<Long, ExercisePose> poseMap = getPoseMap(poseResults);

        return ExerciseStatsResponse.builder()
                .streak(calculateStreak(memberId))
                .sessions(completedSessions.size())
                .completedPoses(calculateCompletedPoses(completedSessions))
                .bestDailyPoses(calculateBestDailyPoses(completedSessions))
                .minutes(calculateTotalMinutes(completedSessions))
                .completion(calculateCompletionRate(sessions, completedSessions))
                .type(buildExerciseType(completedSessions, routineMap, poseResults, poseMap))
                .cal(buildCalendar(period, completedSessions))
                .coaching(buildCoaching(poseResults, poseMap, completedSessions.size()))
                .logs(buildLogs(memberId, from, to, routineMap))
                .build();
    }

    private LocalDateTime getPeriodStart(String period) {
        LocalDate today = LocalDate.now();

        return switch (period) {
            case "day" -> today.atStartOfDay();
            case "week" -> today.minusDays(6).atStartOfDay();
            case "month" -> today.minusDays(29).atStartOfDay();
            default -> throw new IllegalArgumentException("period 값은 day, week, month 중 하나여야 합니다.");
        };
    }

    private boolean isCompleted(ExerciseSession session) {
        return session.getStatus() == SessionStatus.COMPLETED;
    }

    // 세션 목록에서 routineId를 모아 루틴 정보를 한 번에 조회합니다.
    private Map<Long, ExerciseRoutine> getRoutineMap(List<ExerciseSession> sessions) {
        Set<Long> routineIds = sessions.stream()
                .map(ExerciseSession::getRoutineId)
                .collect(Collectors.toSet());

        if (routineIds.isEmpty()) {
            return Map.of();
        }

        return exerciseRoutineRepository.findAllById(routineIds).stream()
                .collect(Collectors.toMap(ExerciseRoutine::getId, Function.identity()));
    }

    private List<PoseResult> getPoseResults(List<ExerciseSession> sessions) {
        List<Long> sessionIds = sessions.stream()
                .map(ExerciseSession::getId)
                .toList();

        if (sessionIds.isEmpty()) {
            return List.of();
        }

        return exercisePoseResultStatsRepository.findBySessionIdIn(sessionIds);
    }

    private Map<Long, ExercisePose> getPoseMap(List<PoseResult> poseResults) {
        Set<Long> poseIds = poseResults.stream()
                .map(PoseResult::getPoseId)
                .collect(Collectors.toSet());

        if (poseIds.isEmpty()) {
            return Map.of();
        }

        return exercisePoseRepository.findAllById(poseIds).stream()
                .collect(Collectors.toMap(ExercisePose::getId, Function.identity()));
    }

    // 오늘부터 거꾸로 보며 연속으로 운동을 완료한 날짜 수를 계산합니다.
    private int calculateStreak(Long memberId) {
        List<ExerciseSession> completedSessions = exerciseStatsRepository
                .findByMemberIdAndStatusOrderByStartedAtDesc(memberId, SessionStatus.COMPLETED);
        Set<LocalDate> exerciseDates = completedSessions.stream()
                .filter(session -> session.getStartedAt() != null)
                .map(session -> session.getStartedAt().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = LocalDate.now();

        while (exerciseDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    private int calculateTotalMinutes(List<ExerciseSession> sessions) {
        long totalSeconds = sessions.stream()
                .mapToLong(this::calculateDurationSeconds)
                .sum();

        return (int) (totalSeconds / 60);
    }

    private int calculateCompletedPoses(List<ExerciseSession> sessions) {
        return sessions.stream()
                .mapToInt(ExerciseSession::getCompletedPoses)
                .sum();
    }

    // 기간 안에서 하루에 가장 많이 완료한 자세 수를 계산합니다.
    private int calculateBestDailyPoses(List<ExerciseSession> sessions) {
        return sessions.stream()
                .filter(session -> session.getStartedAt() != null)
                .collect(Collectors.groupingBy(
                        session -> session.getStartedAt().toLocalDate(),
                        Collectors.summingInt(ExerciseSession::getCompletedPoses)
                ))
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private long calculateDurationSeconds(ExerciseSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) {
            return 0;
        }

        return Math.max(0, Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds());
    }

    private int calculateCompletionRate(List<ExerciseSession> sessions, List<ExerciseSession> completedSessions) {
        if (sessions.isEmpty()) {
            return 0;
        }

        return (int) Math.round(completedSessions.size() * 100.0 / sessions.size());
    }

    private ExerciseTypeResponse buildExerciseType(
            List<ExerciseSession> sessions,
            Map<Long, ExerciseRoutine> routineMap,
            List<PoseResult> poseResults,
            Map<Long, ExercisePose> poseMap
    ) {
        int stretch = 0;
        int yoga = 0;
        Map<Long, List<PoseResult>> resultsBySession = poseResults.stream()
                .collect(Collectors.groupingBy(PoseResult::getSessionId));

        for (ExerciseSession session : sessions) {
            ExerciseCategory category = resolveExerciseCategory(
                    routineMap.get(session.getRoutineId()),
                    resultsBySession.getOrDefault(session.getId(), List.of()),
                    poseMap
            );
            if (category == ExerciseCategory.STRETCH) {
                stretch++;
            } else if (category == ExerciseCategory.YOGA) {
                yoga++;
            }
        }

        return ExerciseTypeResponse.builder()
                .stretch(stretch)
                .yoga(yoga)
                .build();
    }

    private ExerciseCategory resolveExerciseCategory(
            ExerciseRoutine routine,
            List<PoseResult> poseResults,
            Map<Long, ExercisePose> poseMap
    ) {
        if (routine != null && routine.getCategory() != null) {
            return routine.getCategory();
        }

        List<String> poseKeys = poseResults.stream()
                .map(result -> poseMap.get(result.getPoseId()))
                .filter(Objects::nonNull)
                .map(ExercisePose::getPoseKey)
                .toList();
        if (poseKeys.isEmpty()) {
            return ExerciseCategory.STRETCH;
        }

        return poseKeys.stream().allMatch(YOGA_POSE_KEYS::contains)
                ? ExerciseCategory.YOGA
                : ExerciseCategory.STRETCH;
    }

    private List<ExerciseCalendarResponse> buildCalendar(String period, List<ExerciseSession> sessions) {
        LocalDate today = LocalDate.now();
        //캘린더 
        LocalDate fromDate;
        int days;

        if (period.equals("month")) {
            fromDate = today.withDayOfMonth(1);
            days = today.lengthOfMonth();
        } else {
            fromDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
            days = 7;
        }

        Map<LocalDate, Long> countByDate = sessions.stream()
                .filter(session -> session.getStartedAt() != null)
                .collect(Collectors.groupingBy(
                        session -> session.getStartedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<ExerciseCalendarResponse> calendar = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = fromDate.plusDays(i);
            calendar.add(ExerciseCalendarResponse.builder()
                    .d(createCalendarLabel(period, date))
                    .lv(toActivityLevel(countByDate.getOrDefault(date, 0L)))
                    .today(date.equals(today))
                    .build());
        }

        return calendar;
    }

    private String createCalendarLabel(String period, LocalDate date) {
        if (period.equals("month")) {
            return String.valueOf(date.getDayOfMonth());
        }

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

    private int toActivityLevel(long count) {
        if (count <= 0) {
            return 0;
        }
        if (count == 1) {
            return 1;
        }
        if (count == 2) {
            return 2;
        }
        return 3;
    }

    private List<ExerciseCoachingResponse> buildCoaching(
            List<PoseResult> poseResults,
            Map<Long, ExercisePose> poseMap,
            int completedSessionCount
    ) {
        Map<Long, Integer> coachCountByPose = new LinkedHashMap<>();

        poseResults.stream()
                .sorted(Comparator.comparing(PoseResult::getResultOrder))
                .forEach(result -> coachCountByPose.merge(result.getPoseId(), result.getCoachCount(), Integer::sum));

        return coachCountByPose.entrySet().stream()
                .map(entry -> ExerciseCoachingResponse.builder()
                        .name(getPoseName(entry.getKey(), poseMap))
                        .v(toAverageCount(entry.getValue(), completedSessionCount))
                        .build())
                .toList();
    }

    private String getPoseName(Long poseId, Map<Long, ExercisePose> poseMap) {
        ExercisePose pose = poseMap.get(poseId);
        return pose == null ? "알 수 없는 자세" : pose.getNameKo();
    }

    private double toAverageCount(int totalCount, int completedSessionCount) {
        if (completedSessionCount == 0) {
            return 0;
        }

        return Math.round((totalCount / (double) completedSessionCount) * 10) / 10.0;
    }

    private List<ExerciseLogResponse> buildLogs(
            Long memberId,
            LocalDateTime from,
            LocalDateTime to,
            Map<Long, ExerciseRoutine> routineMap
    ) {
        List<ExerciseSession> recentSessions = exerciseStatsRepository
                .findTop5ByMemberIdAndStartedAtBetweenOrderByStartedAtDesc(memberId, from, to);

        return recentSessions.stream()
                .map(session -> buildLog(session, routineMap.get(session.getRoutineId())))
                .toList();
    }

    private ExerciseLogResponse buildLog(ExerciseSession session, ExerciseRoutine routine) {
        List<PoseResult> poseResults = exercisePoseResultStatsRepository.findBySessionId(session.getId());
        int coachCount = poseResults.stream()
                .mapToInt(PoseResult::getCoachCount)
                .sum();

        return ExerciseLogResponse.builder()
                .type(toExerciseTypeText(routine))
                .name(routine == null ? "알 수 없는 루틴" : routine.getName())
                .time(formatPerformedAt(session.getStartedAt()))
                .dur(formatDuration(calculateDurationSeconds(session)))
                .done(session.getCompletedPoses() + "/" + session.getTotalPoses())
                .coach(coachCount)
                .build();
    }

    private String toExerciseTypeText(ExerciseRoutine routine) {
        if (routine == null || routine.getCategory() == null) {
            return "stretch";
        }

        return routine.getCategory() == ExerciseCategory.YOGA ? "yoga" : "stretch";
    }

    private String formatPerformedAt(LocalDateTime performedAt) {
        if (performedAt == null) {
            return "";
        }

        LocalDate date = performedAt.toLocalDate();
        LocalDate today = LocalDate.now();
        String time = performedAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (date.equals(today)) {
            return "오늘 " + time;
        }
        if (date.equals(today.minusDays(1))) {
            return "어제 " + time;
        }

        return performedAt.format(DateTimeFormatter.ofPattern("M/d HH:mm"));
    }

    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return minutes + "분 " + remainingSeconds + "초";
    }
}
