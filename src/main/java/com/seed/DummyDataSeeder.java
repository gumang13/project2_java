package com.seed;

import com.analysis.constant.EventType;
import com.analysis.entity.Analysis;
import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;
import com.analysis.repository.AnalysisRepository;
import com.member.constant.Plan;
import com.member.entity.Member;
import com.member.repository.MemberRepository;
import com.stats.entity.DailyStats;
import com.stats.repository.DailyStatsRepository;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DummyDataSeeder implements ApplicationRunner {

    private static final String TEST_EMAIL = "test@mokgyeokja.com";
    private static final String TEST_PASSWORD = "1234";
    private static final String TEST_NAME = "테스트유저";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AnalysisRepository analysisRepository;
    private final AnalysisEventRepository analysisEventRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final ExerciseStatsRepository exerciseStatsRepository;
    private final ExercisePoseResultStatsRepository poseResultRepository;
    private final ExerciseRoutineRepository routineRepository;
    private final ExercisePoseRepository poseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member member = seedMember();
        Long memberId = member.getId();

        seedMonitorStats(memberId);
        seedExerciseStats(memberId);
    }

// ========================================================================================
// 테스트 회원
// ========================================================================================

    // 로그인 테스트에 바로 사용할 수 있는 회원을 만든다.
    // 이미 같은 이메일 회원이 있으면 새로 만들지 않고 그 회원을 재사용한다.
    private Member seedMember() {
        return memberRepository.findByEmail(TEST_EMAIL)
                .orElseGet(() -> {
                    Member member = new Member();
                    member.setEmail(TEST_EMAIL);
                    member.setPassword(passwordEncoder.encode(TEST_PASSWORD));
                    member.setName(TEST_NAME);
                    member.setPlan(Plan.FREE);
                    return memberRepository.save(member);
                });
    }

// ========================================================================================
// 모니터링 통계 데이터
// ========================================================================================

    // 모니터링 day 화면은 Analysis/AnalysisEvent 원천 데이터로 계산된다.
    // 현재 백엔드 week/month는 DailyStats 타입 이슈가 있어 seed만으로는 검증 대상에서 제외한다.
    private void seedMonitorStats(Long memberId) {
        resetMonitorStats(memberId);
        seedTodayAnalysis(memberId);
        seedRangeMonitorStats(memberId);
    }

    // 테스트 회원의 기존 모니터링 통계만 지운 뒤 오늘 기준으로 다시 만든다.
    private void resetMonitorStats(Long memberId) {
        List<Long> analysisIds = analysisRepository.findIdsByMemberId(memberId);
        if (!analysisIds.isEmpty()) {
            analysisEventRepository.deleteByAnalysisIdIn(analysisIds);
        }

        analysisRepository.deleteByMemberId(memberId);
        dailyStatsRepository.deleteByMemberId(memberId);
    }

    // 프론트 작업용 더미와 비슷하게 65%, 알림 3회, 나쁜 자세 53분, 바른 자세 100분이 나오게 만든다.
    // 전체 측정 시간 153분에서 BAD/GOOD 쌍의 합이 53분이면 goodMinutes는 100분으로 계산된다.
    private void seedTodayAnalysis(Long memberId) {
        LocalDateTime endedAt = LocalDateTime.now().minusMinutes(1).withSecond(0).withNano(0);
        LocalDateTime startedAt = endedAt.minusMinutes(153);

        Analysis analysis = new Analysis();
        analysis.setMemberId(memberId);
        analysis.setStartedAt(startedAt);
        analysis.setEndedAt(endedAt);

        Analysis saved = analysisRepository.save(analysis);

        saveAnalysisEvent(saved.getId(), EventType.BAD_POSTURE, startedAt.plusMinutes(10));
        saveAnalysisEvent(saved.getId(), EventType.GOOD_POSTURE, startedAt.plusMinutes(30));
        saveAnalysisEvent(saved.getId(), EventType.BAD_POSTURE, startedAt.plusMinutes(65));
        saveAnalysisEvent(saved.getId(), EventType.GOOD_POSTURE, startedAt.plusMinutes(80));
        saveAnalysisEvent(saved.getId(), EventType.BAD_POSTURE, startedAt.plusMinutes(120));
        saveAnalysisEvent(saved.getId(), EventType.GOOD_POSTURE, startedAt.plusMinutes(138));

        saveDailyStats(memberId, LocalDate.now(), 153, 53, 3);
    }

    // week/month 화면이 day와 다르게 보이도록 최근 30일 범위의 일별 통계를 추가한다.
    // daily_stats는 카드/막대 그래프 수치에 쓰이고, analysis_event는 시간대 패턴과 로그에 쓰인다.
    private void seedRangeMonitorStats(Long memberId) {
        LocalDate today = LocalDate.now();

        saveMonitorDay(memberId, today.minusDays(1), 150, new int[]{18, 14, 13});
        saveMonitorDay(memberId, today.minusDays(2), 160, new int[]{22, 20, 19});
        saveMonitorDay(memberId, today.minusDays(3), 140, new int[]{12, 11, 12});
        saveMonitorDay(memberId, today.minusDays(5), 155, new int[]{18, 16, 16});
        saveMonitorDay(memberId, today.minusDays(6), 150, new int[]{15, 14, 13});

        saveMonitorDay(memberId, today.minusDays(10), 150, new int[]{24, 22, 22});
        saveMonitorDay(memberId, today.minusDays(15), 150, new int[]{30, 25, 23});
        saveMonitorDay(memberId, today.minusDays(20), 155, new int[]{23, 22, 20});
        saveMonitorDay(memberId, today.minusDays(25), 145, new int[]{25, 25, 20});
        saveMonitorDay(memberId, today.minusDays(28), 150, new int[]{20, 20, 20});
    }

    private void saveMonitorDay(Long memberId, LocalDate date, int totalMinutes, int[] badDurations) {
        LocalDateTime startedAt = date.atTime(9, 0);
        LocalDateTime endedAt = startedAt.plusMinutes(totalMinutes);

        Analysis analysis = new Analysis();
        analysis.setMemberId(memberId);
        analysis.setStartedAt(startedAt);
        analysis.setEndedAt(endedAt);

        Analysis saved = analysisRepository.save(analysis);

        int cursor = 10;
        int badMinutes = 0;
        for (int badDuration : badDurations) {
            badMinutes += badDuration;
            saveAnalysisEvent(saved.getId(), EventType.BAD_POSTURE, startedAt.plusMinutes(cursor));
            saveAnalysisEvent(saved.getId(), EventType.GOOD_POSTURE, startedAt.plusMinutes(cursor + badDuration));
            cursor += badDuration + 25;
        }

        saveDailyStats(memberId, date, totalMinutes, badMinutes, badDurations.length);
    }

    private void saveDailyStats(Long memberId, LocalDate date, int totalMinutes, int badMinutes, int alerts) {
        DailyStats dailyStats = new DailyStats();
        setField(dailyStats, "memberId", memberId);
        setField(dailyStats, "statDate", date.atStartOfDay());
        setField(dailyStats, "notiCount", alerts);
        setField(dailyStats, "goodPostureSec", (totalMinutes - badMinutes) * 60);
        setField(dailyStats, "badPostureSec", badMinutes * 60);
        setField(dailyStats, "totalMeasureSec", totalMinutes * 60);
        dailyStatsRepository.save(dailyStats);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to seed field: " + fieldName, e);
        }
    }

    private void saveAnalysisEvent(Long analysisId, EventType eventType, LocalDateTime eventAt) {
        AnalysisEvent event = new AnalysisEvent();
        event.setAnalysisId(analysisId);
        event.setEventType(eventType);
        event.setEventAt(eventAt);
        analysisEventRepository.save(event);
    }

// ========================================================================================
// 운동 통계 데이터
// ========================================================================================

    // 프론트 작업용 더미와 비슷하게 day/week/month 숫자가 나오도록 세션 수, 시간, 완료율을 구성한다.
    // day: 완료 1회, 약 3분, 완료율 100%
    // week: 완료 5회 + 중단 1회, 총 18분, 완료율 83%
    // month: 완료 21회 + 중단 4회, 총 78분, 완료율 84%
    private void seedExerciseStats(Long memberId) {
        resetExerciseStats(memberId);

        List<ExercisePose> poses = seedExercisePosesIfEmpty();
        ExerciseRoutine stretchRoutine = seedRoutine(memberId, ExerciseCategory.STRETCH, "테스트 스트레칭 루틴", 300);
        ExerciseRoutine yogaRoutine = seedRoutine(memberId, ExerciseCategory.YOGA, "테스트 요가 루틴", 420);

        LocalDate today = LocalDate.now();

        saveExerciseSession(memberId, stretchRoutine, poses, today, 0, 9, 24, 192, true, List.of(1, 0, 2, 0, 4));
        saveExerciseSession(memberId, yogaRoutine, poses, today, 1, 21, 8, 270, true, List.of(2, 1, 2, 1, 3));
        saveExerciseSession(memberId, stretchRoutine, poses, today, 2, 8, 50, 185, true, List.of(1, 1, 1, 1, 1));
        saveExerciseSession(memberId, yogaRoutine, poses, today, 3, 20, 10, 240, true, List.of(1, 1, 2, 1, 2));
        saveExerciseSession(memberId, stretchRoutine, poses, today, 4, 7, 40, 193, true, List.of(2, 0, 1, 0, 2));

        saveExerciseSession(memberId, stretchRoutine, poses, today, 5, 12, 30, 120, false, List.of(1, 0, 1, 0, 1));

        int[] extraDurations = {240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 180, 180, 180, 180};
        for (int i = 0; i < extraDurations.length; i++) {
            ExerciseRoutine routine = i < 9 ? stretchRoutine : yogaRoutine;
            saveExerciseSession(
                    memberId,
                    routine,
                    poses,
                    today,
                    i + 7,
                    8 + (i % 5),
                    15,
                    extraDurations[i],
                    true,
                    List.of(1 + (i % 2), i % 2, 2 + (i % 3), 1, 2)
            );
        }

        saveExerciseSession(memberId, yogaRoutine, poses, today, 23, 18, 20, 150, false, List.of(1, 1, 1, 0, 1));
        saveExerciseSession(memberId, stretchRoutine, poses, today, 24, 19, 5, 140, false, List.of(1, 0, 1, 0, 0));
        saveExerciseSession(memberId, yogaRoutine, poses, today, 25, 10, 45, 130, false, List.of(0, 1, 1, 0, 1));
    }

    private void saveExerciseSession(
            Long memberId,
            ExerciseRoutine routine,
            List<ExercisePose> poses,
            LocalDate today,
            int daysAgo,
            int hour,
            int minute,
            int durationSeconds,
            boolean completed,
            List<Integer> coachCounts
    ) {
        int totalPoses = Math.min(poses.size(), 5);
        int completedPoses = completed ? totalPoses : Math.max(0, totalPoses - 1);
        LocalDateTime startedAt = today.minusDays(daysAgo).atTime(hour, minute);

        ExerciseSession session = new ExerciseSession();
        session.setMemberId(memberId);
        session.setRoutineId(routine.getId());
        session.setStatus(completed ? SessionStatus.COMPLETED : SessionStatus.ABANDONED);
        session.setStartedAt(startedAt);
        session.setEndedAt(startedAt.plusSeconds(durationSeconds));
        session.setTotalPoses(totalPoses);
        session.setCompletedPoses(completedPoses);

        ExerciseSession savedSession = exerciseStatsRepository.save(session);

        for (int order = 0; order < totalPoses; order++) {
            ExercisePose pose = poses.get(order);
            PoseResult result = new PoseResult();
            result.setSessionId(savedSession.getId());
            result.setPoseId(pose.getId());
            result.setResultOrder(order);
            result.setCompleted(order < completedPoses);
            result.setCoachCount(order < coachCounts.size() ? coachCounts.get(order) : 0);
            result.setHoldAchievedSec(Math.max(5, pose.getHoldSec() - (order % 3)));
            poseResultRepository.save(result);
        }
    }

    // 테스트 회원의 기존 운동 세션과 자세 결과를 지운다.
    private void resetExerciseStats(Long memberId) {
        LocalDateTime from = LocalDateTime.now().minusDays(90);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        List<ExerciseSession> sessions =
                exerciseStatsRepository.findByMemberIdAndStartedAtBetweenOrderByStartedAtAsc(memberId, from, to);
        List<Long> sessionIds = sessions.stream().map(ExerciseSession::getId).toList();

        if (!sessionIds.isEmpty()) {
            poseResultRepository.deleteAll(poseResultRepository.findBySessionIdIn(sessionIds));
        }

        exerciseStatsRepository.deleteAll(sessions);
    }

    // 기존 스트레칭 시더가 먼저 데이터를 넣었다면 그 자세를 사용하고,
    // 비어 있으면 운동 통계용 최소 자세를 만든다.
    private List<ExercisePose> seedExercisePosesIfEmpty() {
        List<ExercisePose> poses = poseRepository.findAll();
        if (!poses.isEmpty()) {
            return poses;
        }

        List<ExercisePose> created = new ArrayList<>();
        created.add(savePose("턱 당기기", "Chin Tuck", 10, false));
        created.add(savePose("목 뒤로 젖히기", "Neck Extension", 8, false));
        created.add(savePose("앉아서 앞으로 숙이기", "Seated Forward Bend", 15, false));
        created.add(savePose("스탠딩 폴드", "Standing Forward Fold", 15, false));
        created.add(savePose("로우 런지", "Low Lunge", 12, true));
        return created;
    }

    private ExercisePose savePose(String nameKo, String nameEn, int holdSec, boolean bilateral) {
        ExercisePose pose = new ExercisePose();
        pose.setNameKo(nameKo);
        pose.setNameEn(nameEn);
        pose.setHoldSec(holdSec);
        pose.setBilateral(bilateral);
        pose.setIntroTts(nameKo + " 자세를 시작합니다.");
        return poseRepository.save(pose);
    }

    private ExerciseRoutine seedRoutine(
            Long memberId,
            ExerciseCategory category,
            String name,
            int estSeconds
    ) {
        ExerciseRoutine routine = new ExerciseRoutine();
        routine.setMemberId(memberId);
        routine.setCategory(category);
        routine.setName(name);
        routine.setDescription("통계 화면 확인용 테스트 루틴입니다.");
        routine.setDifficulty(1);
        routine.setEstSeconds(estSeconds);
        return routineRepository.save(routine);
    }

}
