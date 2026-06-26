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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseStatsService {

    private final ExerciseStatsRepository exerciseStatsRepository;
    private final ExercisePoseResultStatsRepository exercisePoseResultStatsRepository;
    private final ExerciseRoutineRepository exerciseRoutineRepository;
    private final ExercisePoseRepository exercisePoseRepository;

    @Transactional(readOnly = true)
    public ExerciseStatsResponse getExerciseStats(String period, Long memberId) {
//==========================================================================================================
//==========================================================================================================


        // 1. 요청한 period 기준으로 DB 조회 기간
        LocalDateTime from = getPeriodStart(period);
        LocalDateTime to = LocalDateTime.now();

        // 2. 로그인한 회원의 요청 기간 내 운동 세션을 오래된 순서로 조회
        List<ExerciseSession> sessions = exerciseStatsRepository
                .findByMemberIdAndStartedAtBetweenOrderByStartedAtAsc(memberId, from, to);

        // 2-1. 화면의 운동 횟수, 시간, 종류 비중, 캘린더는 완료 세션 기준으로 계산
        List<ExerciseSession> completedSessions = sessions.stream()
                .filter(this::isCompleted)
                .toList();

        // 3. 루틴 이름/category 조회용 Map
        Map<Long, ExerciseRoutine> routineMap = getRoutineMap(sessions);

        // 3-1. 완료 세션들의 자세별 결과
        List<PoseResult> poseResults = getPoseResults(completedSessions);

        // 3-2. 자세 이름 표시용
        Map<Long, ExercisePose> poseMap = getPoseMap(poseResults);



        // 9. 프론트 운동 통계 화면에 응답
        return ExerciseStatsResponse.builder()
                .streak(calculateStreak(memberId))
                .sessions(completedSessions.size())
                .minutes(calculateTotalMinutes(completedSessions))
                .completion(calculateCompletionRate(sessions, completedSessions))
                .type(buildExerciseType(completedSessions, routineMap))
                .cal(buildCalendar(period, completedSessions))
                .coaching(buildCoaching(poseResults, poseMap, completedSessions.size()))
                .logs(buildLogs(memberId, from, to, routineMap))
                .build();
    }


//===========================================================================================================
//===========================================================================================================


    // 1-1. period 값에 따라 통계 조회 시작 시각을 계산
    private LocalDateTime getPeriodStart(String period) {
        LocalDate today = LocalDate.now();

        return switch (period) {
            case "day" -> today.atStartOfDay();
            case "week" -> today.minusDays(6).atStartOfDay();
            case "month" -> today.minusDays(29).atStartOfDay();
            default -> throw new IllegalArgumentException("period 값은 day, week, month 중 하나여야 합니다.");
        };
    }


//==========================================================================================================
//==========================================================================================================


    // 2-2. 완료 세션인지 확인
    private boolean isCompleted(ExerciseSession session) {
        return session.getStatus() == SessionStatus.COMPLETED;
    }


//==========================================================================================================
//==========================================================================================================


    // 3-3. 세션 목록에서 routineId를 모아 루틴 정보를 한 번에 조회합니다.
    //      반복문 안에서 findById를 여러 번 호출하지 않기 위한 준비 작업입니다.
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



    // 3-4. 완료 세션 id 목록으로 자세별 결과를 한 번에 조회합니다.
    private List<PoseResult> getPoseResults(List<ExerciseSession> sessions) {
        List<Long> sessionIds = sessions.stream()
                .map(ExerciseSession::getId)
                .toList();

        if (sessionIds.isEmpty()) {
            return List.of();
        }

        return exercisePoseResultStatsRepository.findBySessionIdIn(sessionIds);
    }



    // 3-5. poseResult에 들어있는 poseId로 자세 이름 정보를 한 번에 조회합니다.
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


//==========================================================================================================
//==========================================================================================================


    // 4-1. 오늘부터 거꾸로 보면서 연속으로 완료 운동이 있었던 날짜 수를 계산합니다.
    //      예: 오늘, 어제, 그제 운동했고 3일 전은 없으면 streak = 3
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


    // 4-2. 완료 세션들의 총 운동 시간을 분 단위로 계산합니다.
    private int calculateTotalMinutes(List<ExerciseSession> sessions) {
        long totalSeconds = sessions.stream()
                .mapToLong(this::calculateDurationSeconds)
                .sum();

        return (int) (totalSeconds / 60);
    }


    // 4-3. 세션 하나의 시작~종료 시간을 초 단위로 계산합니다.
    //      종료 시간이 없는 세션은 통계 시간에 포함하지 않습니다.
    private long calculateDurationSeconds(ExerciseSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) {
            return 0;
        }

        return Math.max(0, Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds());
    }



    // 4-4. 완주율 = 완료 세션 수 / 시작한 전체 세션 수 * 100
    private int calculateCompletionRate(List<ExerciseSession> sessions, List<ExerciseSession> completedSessions) {
        if (sessions.isEmpty()) {
            return 0;
        }

        return (int) Math.round(completedSessions.size() * 100.0 / sessions.size());
    }


//==========================================================================================================
//==========================================================================================================


    // 5-1. 완료 세션의 루틴 category를 보고 스트레칭/요가 횟수를 셉니다.
    private ExerciseTypeResponse buildExerciseType(
            List<ExerciseSession> sessions,
            Map<Long, ExerciseRoutine> routineMap
    ) {
        int stretch = 0;
        int yoga = 0;

        for (ExerciseSession session : sessions) {
            ExerciseRoutine routine = routineMap.get(session.getRoutineId());
            if (routine == null || routine.getCategory() == null) {
                continue;
            }

            if (routine.getCategory() == ExerciseCategory.STRETCH) {
                stretch++;
            } else if (routine.getCategory() == ExerciseCategory.YOGA) {
                yoga++;
            }
        }

        return ExerciseTypeResponse.builder()
                .stretch(stretch)
                .yoga(yoga)
                .build();
    }


//==========================================================================================================
//==========================================================================================================


    // 6-1. 운동 캘린더에 표시할 날짜별 운동량을 만듭니다.
    //      week/day는 7칸, month는 30칸으로 구성합니다.
    private List<ExerciseCalendarResponse> buildCalendar(String period, List<ExerciseSession> sessions) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = period.equals("month") ? today.minusDays(29) : today.minusDays(6);
        int days = period.equals("month") ? 30 : 7;

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


    // 6-2. 캘린더 셀 안에 표시할 라벨입니다.
    //      month는 날짜 숫자, day/week는 요일 한 글자를 사용합니다.
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



    // 6-3. 하루 운동 세션 수를 잔디 색상 단계로 바꿉니다.
    //      0: 없음, 1: 적음, 2: 보통, 3: 많음
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


//==========================================================================================================
//==========================================================================================================


    // 7-1. 자세별 코칭 횟수를 계산합니다.
    //      같은 자세가 여러 세션에 나오면 coachCount를 합산한 뒤 완료 세션 수로 나눠 평균을 냅니다.
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



    // 7-2. poseId에 해당하는 자세 이름을 가져옵니다.
    private String getPoseName(Long poseId, Map<Long, ExercisePose> poseMap) {
        ExercisePose pose = poseMap.get(poseId);
        return pose == null ? "알 수 없는 자세" : pose.getNameKo();
    }



    // 7-3. 합산 코칭 횟수를 세션당 평균으로 바꿉니다.
    //      소수 첫째 자리까지 반올림합니다.
    private double toAverageCount(int totalCount, int completedSessionCount) {
        if (completedSessionCount == 0) {
            return 0;
        }

        return Math.round((totalCount / (double) completedSessionCount) * 10) / 10.0;
    }


//==========================================================================================================
//==========================================================================================================


    // 8-1. 기간 내 최근 운동 세션 5개를 로그 DTO로 변환합니다.
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


    // 8-2. 세션 하나를 최근 운동 로그 한 줄로 변환합니다.
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


    // 8-3. 프론트 배지 class와 맞추기 위해 stretch/yoga 문자열로 변환합니다.
    private String toExerciseTypeText(ExerciseRoutine routine) {
        if (routine == null || routine.getCategory() == null) {
            return "stretch";
        }

        return routine.getCategory() == ExerciseCategory.YOGA ? "yoga" : "stretch";
    }



    // 8-4. 최근 운동 로그에 표시할 시간 문구를 만듭니다.
    //      오늘/어제는 한글로, 그 외 날짜는 M/d HH:mm 형식으로 표시합니다.
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


    // 8-5. 초 단위 운동 시간을 "n분 n초" 표시 문자열로 바꿉니다.
    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return minutes + "분 " + remainingSeconds + "초";
    }
}
