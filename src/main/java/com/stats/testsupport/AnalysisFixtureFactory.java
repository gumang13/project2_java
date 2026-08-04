package com.stats.testsupport;

import com.analysis.constant.EventType;
import com.analysis.entity.Analysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 통계 성능 측정 / 회귀 테스트용 결정적(deterministic) 더미 데이터 생성기.
 *
 * <p>Spring 의존성이 없다. 순수 계산이라 단위 테스트로 이 클래스 자체를 검증할 수 있고,
 * 실제 저장은 {@code AnalysisDataSeeder}가 담당한다.
 *
 * <p><b>중요:</b> AnalysisEvent.analysisId 는 원시 Long 이라 Analysis 를 먼저 저장해
 * id 를 받아야 이벤트를 만들 수 있다. 그래서 이벤트는 {@link EventDraft} 라는
 * "analysisId 가 아직 없는" 중간 형태로 만들어 두고, 시더가 저장 후 채운다.
 */
public final class AnalysisFixtureFactory {

    private AnalysisFixtureFactory() {
    }

    // ---------------------------------------------------------------------
    // 결과 타입
    // ---------------------------------------------------------------------

    /** analysisId 가 아직 정해지지 않은 이벤트. */
    public record EventDraft(EventType type, LocalDateTime at, Double angle) {
    }

    /** 세션 1개 + 그 세션에 속한 이벤트들. */
    public record SessionDraft(Analysis analysis, List<EventDraft> events) {
    }

    /**
     * 하루치 생성 규칙.
     *
     * @param sessions        그날 만들 세션 수
     * @param pairsPerSession 세션당 BAD→GOOD 쌍 개수 (이벤트 수 = 쌍 x 2)
     * @param goodGapMinSec   바른 자세 유지 시간 하한 (다음 BAD 까지의 간격)
     * @param goodGapMaxSec   바른 자세 유지 시간 상한
     * @param badMinSec       거북목 지속 시간 하한
     * @param badMaxSec       거북목 지속 시간 상한
     * @param startHour       그날 첫 세션 시작 시각(시)
     */
    public record DaySpec(
            int sessions,
            int pairsPerSession,
            int goodGapMinSec,
            int goodGapMaxSec,
            int badMinSec,
            int badMaxSec,
            int startHour
    ) {
        /** 평범한 하루: 3세션 x 80이벤트 = 240건. 30일이면 약 7,200건. */
        public static DaySpec normal() {
            return new DaySpec(3, 40, 60, 300, 30, 180, 9);
        }

        /**
         * ESP32 연결이 자주 끊긴 날. 짧은 세션이 잔뜩 생긴다.
         * day 경로의 중첩 루프 O(A x E) 를 실제로 측정하려면 이 모양이 필요하다.
         * 20세션 x 160이벤트 = 3,200건이 "하루 안에" 몰린다.
         */
        public static DaySpec flakyConnection() {
            return new DaySpec(20, 80, 8, 45, 5, 30, 8);
        }
    }

    // ---------------------------------------------------------------------
    // 프리셋
    // ---------------------------------------------------------------------

    /**
     * 30일치 표준 시나리오. 마지막 날만 flakyConnection 으로 채워서
     * day 경로(중첩 루프)와 month 경로(전량 로딩)를 한 번에 측정할 수 있게 한다.
     */
    public static List<SessionDraft> monthWithHeavyToday(long memberId, LocalDate today, long seed) {
        List<SessionDraft> all = new ArrayList<>();

        // D-29 ~ D-1 : 평범한 날
        for (int back = 29; back >= 1; back--) {
            all.addAll(buildDay(memberId, today.minusDays(back), DaySpec.normal(), seed + back));
        }
        // D-0 : 오늘은 세션이 많은 날
        all.addAll(buildDay(memberId, today, DaySpec.flakyConnection(), seed));

        return all;
    }

    /** 30일 전부 평범한 날. 순수하게 month 경로만 볼 때 사용. */
    public static List<SessionDraft> flatMonth(long memberId, LocalDate today, long seed) {
        List<SessionDraft> all = new ArrayList<>();
        for (int back = 29; back >= 0; back--) {
            all.addAll(buildDay(memberId, today.minusDays(back), DaySpec.normal(), seed + back));
        }
        return all;
    }

    // ---------------------------------------------------------------------
    // 하루치 생성
    // ---------------------------------------------------------------------

    public static List<SessionDraft> buildDay(long memberId, LocalDate date, DaySpec spec, long seed) {
        List<SessionDraft> drafts = new ArrayList<>();
        LocalDateTime cursor = date.atStartOfDay().plusHours(spec.startHour());

        for (int i = 0; i < spec.sessions(); i++) {
            // 세션마다 별도 시드. 세션 수를 바꿔도 앞쪽 세션의 값이 흔들리지 않는다.
            Random random = new Random(seed * 1_000_003L + i);

            List<EventDraft> events = new ArrayList<>();
            LocalDateTime sessionStart = cursor;
            LocalDateTime at = sessionStart;

            for (int p = 0; p < spec.pairsPerSession(); p++) {
                at = at.plusSeconds(between(random, spec.goodGapMinSec(), spec.goodGapMaxSec()));
                events.add(new EventDraft(EventType.BAD_POSTURE, at, badAngle(random)));

                at = at.plusSeconds(between(random, spec.badMinSec(), spec.badMaxSec()));
                events.add(new EventDraft(EventType.GOOD_POSTURE, at, goodAngle(random)));
            }

            LocalDateTime sessionEnd = at.plusSeconds(between(random, 30, 300));

            Analysis analysis = new Analysis();
            analysis.setMemberId(memberId);
            analysis.setStartedAt(sessionStart);
            analysis.setEndedAt(sessionEnd);

            drafts.add(new SessionDraft(analysis, events));

            // 다음 세션까지 쉬는 시간
            cursor = sessionEnd.plusSeconds(between(random, 300, 2400));

            // 자정을 넘기면 그날 생성은 중단한다 (기간 경계 오염 방지)
            if (!cursor.toLocalDate().equals(date)) {
                break;
            }
        }

        return drafts;
    }

    // ---------------------------------------------------------------------
    // 경계 케이스 fixture
    //
    // 대량 데이터와 절대 섞지 말 것. 랜덤 데이터 위에서는 기대값을 손으로 계산할 수
    // 없어서, 회귀 테스트가 "자기 출력을 그대로 베끼는" 무의미한 테스트가 된다.
    // 정확한 값 검증은 여기서만 한다.
    // ---------------------------------------------------------------------

    /** 조회 기간 시작 이전에 시작해서 안쪽으로 걸친 세션. clipping 검증용. */
    public static SessionDraft straddlingStart(long memberId, LocalDateTime from) {
        Analysis a = session(memberId, from.minusHours(2), from.plusHours(1));
        return new SessionDraft(a, List.of(
                new EventDraft(EventType.BAD_POSTURE, from.minusMinutes(30), 55.0),
                new EventDraft(EventType.GOOD_POSTURE, from.plusMinutes(30), 15.0)
        ));
        // 기대값: BAD 총 60분 중 기간 안쪽 30분만 집계되어야 한다.
    }

    /** 조회 기간 끝을 넘어가는 세션. */
    public static SessionDraft straddlingEnd(long memberId, LocalDateTime to) {
        Analysis a = session(memberId, to.minusHours(1), to.plusHours(2));
        return new SessionDraft(a, List.of(
                new EventDraft(EventType.BAD_POSTURE, to.minusMinutes(20), 60.0),
                new EventDraft(EventType.GOOD_POSTURE, to.plusMinutes(40), 12.0)
        ));
        // 기대값: BAD 총 60분 중 기간 안쪽 20분만.
    }

    /** endedAt 이 null 인 진행 중 세션. to 까지 계산되어야 한다. */
    public static SessionDraft openSession(long memberId, LocalDateTime startedAt, LocalDateTime badAt) {
        Analysis a = session(memberId, startedAt, null);
        return new SessionDraft(a, List.of(
                new EventDraft(EventType.BAD_POSTURE, badAt, 58.0)
        ));
    }

    /** BAD 로 시작했는데 닫는 GOOD 없이 세션이 끝난 경우. sessionEnd 까지만 반영되어야 한다. */
    public static SessionDraft unclosedBadPosture(long memberId, LocalDateTime startedAt, LocalDateTime endedAt) {
        Analysis a = session(memberId, startedAt, endedAt);
        return new SessionDraft(a, List.of(
                new EventDraft(EventType.BAD_POSTURE, startedAt.plusMinutes(10), 62.0)
        ));
    }

    /** 이벤트가 하나도 없는 세션. 측정 시간은 잡히고 BAD 는 0 이어야 한다. */
    public static SessionDraft emptySession(long memberId, LocalDateTime startedAt, LocalDateTime endedAt) {
        return new SessionDraft(session(memberId, startedAt, endedAt), List.of());
    }

    /**
     * 두 세션이 시간상 겹칠 때 서로 섞이지 않는지(세션별 격리) 검증용.
     * A 세션의 BAD 가 열린 상태에서 B 세션의 GOOD 이 와도 A 가 닫히면 안 된다.
     */
    public static List<SessionDraft> interleavedSessions(long memberId, LocalDateTime base) {
        Analysis a = session(memberId, base, base.plusHours(2));
        Analysis b = session(memberId, base.plusMinutes(30), base.plusHours(2));

        return List.of(
                new SessionDraft(a, List.of(
                        new EventDraft(EventType.BAD_POSTURE, base.plusMinutes(10), 55.0),
                        new EventDraft(EventType.GOOD_POSTURE, base.plusMinutes(70), 14.0)
                )),
                new SessionDraft(b, List.of(
                        new EventDraft(EventType.BAD_POSTURE, base.plusMinutes(40), 57.0),
                        new EventDraft(EventType.GOOD_POSTURE, base.plusMinutes(50), 13.0)
                ))
        );
        // 기대값: A 는 60분, B 는 10분. 합 70분.
    }

    // ---------------------------------------------------------------------
    // 내부 헬퍼
    // ---------------------------------------------------------------------

    private static Analysis session(long memberId, LocalDateTime startedAt, LocalDateTime endedAt) {
        Analysis a = new Analysis();
        a.setMemberId(memberId);
        a.setStartedAt(startedAt);
        a.setEndedAt(endedAt);
        return a;
    }

    private static int between(Random random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    /** 거북목 구간의 경추 각도(도). 오픈 통계 API 집계용. */
    private static double badAngle(Random random) {
        return round1(45.0 + random.nextDouble() * 25.0);
    }

    /** 바른 자세 구간의 경추 각도(도). */
    private static double goodAngle(Random random) {
        return round1(8.0 + random.nextDouble() * 17.0);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
