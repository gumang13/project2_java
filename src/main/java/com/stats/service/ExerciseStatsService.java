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


        // 1. ?붿껌??period 湲곗??쇰줈 DB 議고쉶 湲곌컙
        LocalDateTime from = getPeriodStart(period);
        LocalDateTime to = LocalDateTime.now();

        // 2. 濡쒓렇?명븳 ?뚯썝???붿껌 湲곌컙 ???대룞 ?몄뀡???ㅻ옒???쒖꽌濡?議고쉶
        List<ExerciseSession> sessions = exerciseStatsRepository
                .findByMemberIdAndStartedAtBetweenOrderByStartedAtAsc(memberId, from, to);

        // 2-1. ?붾㈃???대룞 ?잛닔, ?쒓컙, 醫낅쪟 鍮꾩쨷, 罹섎┛?붾뒗 ?꾨즺 ?몄뀡 湲곗??쇰줈 怨꾩궛
        List<ExerciseSession> completedSessions = sessions.stream()
                .filter(this::isCompleted)
                .toList();

        // 3. 猷⑦떞 ?대쫫/category 議고쉶??Map
        Map<Long, ExerciseRoutine> routineMap = getRoutineMap(sessions);

        // 3-1. ?꾨즺 ?몄뀡?ㅼ쓽 ?먯꽭蹂?寃곌낵
        List<PoseResult> poseResults = getPoseResults(completedSessions);

        // 3-2. ?먯꽭 ?대쫫 ?쒖떆??
        Map<Long, ExercisePose> poseMap = getPoseMap(poseResults);



        // 9. ?꾨줎???대룞 ?듦퀎 ?붾㈃???묐떟
        return ExerciseStatsResponse.builder()
                .streak(calculateStreak(memberId))
                .sessions(completedSessions.size())
                .completedPoses(calculateCompletedPoses(completedSessions))
                .bestDailyPoses(calculateBestDailyPoses(completedSessions))
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


    // 1-1. period 媛믪뿉 ?곕씪 ?듦퀎 議고쉶 ?쒖옉 ?쒓컖??怨꾩궛
    private LocalDateTime getPeriodStart(String period) {
        LocalDate today = LocalDate.now();

        return switch (period) {
            case "day" -> today.atStartOfDay();
            case "week" -> today.minusDays(6).atStartOfDay();
            case "month" -> today.minusDays(29).atStartOfDay();
            default -> throw new IllegalArgumentException("period 媛믪? day, week, month 以??섎굹?ъ빞 ?⑸땲??");
        };
    }


//==========================================================================================================
//==========================================================================================================


    // 2-2. ?꾨즺 ?몄뀡?몄? ?뺤씤
    private boolean isCompleted(ExerciseSession session) {
        return session.getStatus() == SessionStatus.COMPLETED;
    }


//==========================================================================================================
//==========================================================================================================


    // 3-3. ?몄뀡 紐⑸줉?먯꽌 routineId瑜?紐⑥븘 猷⑦떞 ?뺣낫瑜???踰덉뿉 議고쉶?⑸땲??
    //      諛섎났臾??덉뿉??findById瑜??щ윭 踰??몄텧?섏? ?딄린 ?꾪븳 以鍮??묒뾽?낅땲??
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



    // 3-4. ?꾨즺 ?몄뀡 id 紐⑸줉?쇰줈 ?먯꽭蹂?寃곌낵瑜???踰덉뿉 議고쉶?⑸땲??
    private List<PoseResult> getPoseResults(List<ExerciseSession> sessions) {
        List<Long> sessionIds = sessions.stream()
                .map(ExerciseSession::getId)
                .toList();

        if (sessionIds.isEmpty()) {
            return List.of();
        }

        return exercisePoseResultStatsRepository.findBySessionIdIn(sessionIds);
    }



    // 3-5. poseResult???ㅼ뼱?덈뒗 poseId濡??먯꽭 ?대쫫 ?뺣낫瑜???踰덉뿉 議고쉶?⑸땲??
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


    // 4-1. ?ㅻ뒛遺??嫄곌씀濡?蹂대㈃???곗냽?쇰줈 ?꾨즺 ?대룞???덉뿀???좎쭨 ?섎? 怨꾩궛?⑸땲??
    //      ?? ?ㅻ뒛, ?댁젣, 洹몄젣 ?대룞?덇퀬 3???꾩? ?놁쑝硫?streak = 3
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


    // 4-2. ?꾨즺 ?몄뀡?ㅼ쓽 珥??대룞 ?쒓컙??遺??⑥쐞濡?怨꾩궛?⑸땲??
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


    // 4-3. ?몄뀡 ?섎굹???쒖옉~醫낅즺 ?쒓컙??珥??⑥쐞濡?怨꾩궛?⑸땲??
    //      醫낅즺 ?쒓컙???녿뒗 ?몄뀡? ?듦퀎 ?쒓컙???ы븿?섏? ?딆뒿?덈떎.
    private long calculateDurationSeconds(ExerciseSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) {
            return 0;
        }

        return Math.max(0, Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds());
    }



    // 4-4. ?꾩＜??= ?꾨즺 ?몄뀡 ??/ ?쒖옉???꾩껜 ?몄뀡 ??* 100
    private int calculateCompletionRate(List<ExerciseSession> sessions, List<ExerciseSession> completedSessions) {
        if (sessions.isEmpty()) {
            return 0;
        }

        return (int) Math.round(completedSessions.size() * 100.0 / sessions.size());
    }


//==========================================================================================================
//==========================================================================================================


    // 5-1. ?꾨즺 ?몄뀡??猷⑦떞 category瑜?蹂닿퀬 ?ㅽ듃?덉묶/?붽? ?잛닔瑜??됰땲??
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


    // 6-1. ?대룞 罹섎┛?붿뿉 ?쒖떆???좎쭨蹂??대룞?됱쓣 留뚮벊?덈떎.
    //      week/day??7移? month??30移몄쑝濡?援ъ꽦?⑸땲??
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


    // 6-2. 罹섎┛??? ?덉뿉 ?쒖떆???쇰꺼?낅땲??
    //      month???좎쭨 ?レ옄, day/week???붿씪 ??湲?먮? ?ъ슜?⑸땲??
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



    // 6-3. ?섎（ ?대룞 ?몄뀡 ?섎? ?붾뵒 ?됱긽 ?④퀎濡?諛붽퓠?덈떎.
    //      0: ?놁쓬, 1: ?곸쓬, 2: 蹂댄넻, 3: 留롮쓬
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


    // 7-1. ?먯꽭蹂?肄붿묶 ?잛닔瑜?怨꾩궛?⑸땲??
    //      媛숈? ?먯꽭媛 ?щ윭 ?몄뀡???섏삤硫?coachCount瑜??⑹궛?????꾨즺 ?몄뀡 ?섎줈 ?섎닠 ?됯퇏???낅땲??
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



    // 7-2. poseId???대떦?섎뒗 ?먯꽭 ?대쫫??媛?몄샃?덈떎.
    private String getPoseName(Long poseId, Map<Long, ExercisePose> poseMap) {
        ExercisePose pose = poseMap.get(poseId);
        return pose == null ? "?????녿뒗 ?먯꽭" : pose.getNameKo();
    }



    // 7-3. ?⑹궛 肄붿묶 ?잛닔瑜??몄뀡???됯퇏?쇰줈 諛붽퓠?덈떎.
    //      ?뚯닔 泥レ㎏ ?먮━源뚯? 諛섏삱由쇳빀?덈떎.
    private double toAverageCount(int totalCount, int completedSessionCount) {
        if (completedSessionCount == 0) {
            return 0;
        }

        return Math.round((totalCount / (double) completedSessionCount) * 10) / 10.0;
    }


//==========================================================================================================
//==========================================================================================================


    // 8-1. 湲곌컙 ??理쒓렐 ?대룞 ?몄뀡 5媛쒕? 濡쒓렇 DTO濡?蹂?섑빀?덈떎.
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


    // 8-2. ?몄뀡 ?섎굹瑜?理쒓렐 ?대룞 濡쒓렇 ??以꾨줈 蹂?섑빀?덈떎.
    private ExerciseLogResponse buildLog(ExerciseSession session, ExerciseRoutine routine) {
        List<PoseResult> poseResults = exercisePoseResultStatsRepository.findBySessionId(session.getId());
        int coachCount = poseResults.stream()
                .mapToInt(PoseResult::getCoachCount)
                .sum();

        return ExerciseLogResponse.builder()
                .type(toExerciseTypeText(routine))
                .name(routine == null ? "?????녿뒗 猷⑦떞" : routine.getName())
                .time(formatPerformedAt(session.getStartedAt()))
                .dur(formatDuration(calculateDurationSeconds(session)))
                .done(session.getCompletedPoses() + "/" + session.getTotalPoses())
                .coach(coachCount)
                .build();
    }


    // 8-3. ?꾨줎??諛곗? class? 留욎텛湲??꾪빐 stretch/yoga 臾몄옄?대줈 蹂?섑빀?덈떎.
    private String toExerciseTypeText(ExerciseRoutine routine) {
        if (routine == null || routine.getCategory() == null) {
            return "stretch";
        }

        return routine.getCategory() == ExerciseCategory.YOGA ? "yoga" : "stretch";
    }



    // 8-4. 理쒓렐 ?대룞 濡쒓렇???쒖떆???쒓컙 臾멸뎄瑜?留뚮벊?덈떎.
    //      ?ㅻ뒛/?댁젣???쒓?濡? 洹????좎쭨??M/d HH:mm ?뺤떇?쇰줈 ?쒖떆?⑸땲??
    private String formatPerformedAt(LocalDateTime performedAt) {
        if (performedAt == null) {
            return "";
        }

        LocalDate date = performedAt.toLocalDate();
        LocalDate today = LocalDate.now();
        String time = performedAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (date.equals(today)) {
            return "?ㅻ뒛 " + time;
        }
        if (date.equals(today.minusDays(1))) {
            return "?댁젣 " + time;
        }

        return performedAt.format(DateTimeFormatter.ofPattern("M/d HH:mm"));
    }


    // 8-5. 珥??⑥쐞 ?대룞 ?쒓컙??"n遺?n珥? ?쒖떆 臾몄옄?대줈 諛붽퓠?덈떎.
    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return minutes + "분 " + remainingSeconds + "초";
    }
}

