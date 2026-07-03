package com.openstats.service;

import com.analysis.entity.Analysis;
import com.analysis.entity.AnalysisEvent;
import com.analysis.repository.AnalysisEventRepository;
import com.analysis.repository.AnalysisRepository;
import com.member.entity.Member;
import com.member.repository.MemberRepository;
import com.openstats.entity.SegmentCohortStats;
import com.openstats.entity.SegmentPostureStats;
import com.openstats.repository.SegmentCohortStatsRepository;
import com.openstats.repository.SegmentPostureStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 오픈 통계 배치 — raw 이벤트를 세그먼트/코호트 집계 테이블로 미리 구워둔다.
 * 관리자 트리거(POST /api/v1/admin/aggregate)로 1회 실행. 전량 재계산(delete+insert) 방식.
 */
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final AnalysisEventRepository eventRepository;
    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;
    private final SegmentPostureStatsRepository postureRepository;
    private final SegmentCohortStatsRepository cohortRepository;

    // 경추 각도 → 상태 구간 (더미 각도 기준. 낮을수록 좋음)
    private static final double GOOD_MAX = 16.0;   // < 16 : good
    private static final double WATCH_MAX = 21.0;  // 16~21 : watching, > 21 : alert

    @Transactional
    public int aggregate() {
        List<AnalysisEvent> events = eventRepository.findAll();

        // analysisId → memberId
        Map<Long, Long> memberByAnalysis = new HashMap<>();
        for (Analysis a : analysisRepository.findAll()) {
            memberByAnalysis.put(a.getId(), a.getMemberId());
        }
        // memberId → Member(인구통계)
        Map<Long, Member> memberById = new HashMap<>();
        for (Member m : memberRepository.findAll()) {
            memberById.put(m.getId(), m);
        }
        // memberId → 첫 사용 시각 (MIN eventAt) — 코호트 주차 기준점
        Map<Long, LocalDateTime> firstSeen = new HashMap<>();
        for (AnalysisEvent e : events) {
            Long mid = memberByAnalysis.get(e.getAnalysisId());
            if (mid == null || e.getEventAt() == null) continue;
            firstSeen.merge(mid, e.getEventAt(), (x, y) -> x.isBefore(y) ? x : y);
        }

        Map<String, PostureAcc> pmap = new HashMap<>();   // ageGroup|gender|region|period
        Map<String, CohortAcc> cmap = new HashMap<>();     // ageGroup|gender|week

        for (AnalysisEvent e : events) {
            Long mid = memberByAnalysis.get(e.getAnalysisId());
            if (mid == null) continue;
            Member m = memberById.get(mid);
            if (m == null || m.getBirthYear() == null || m.getGender() == null) continue;
            Double angle = e.getAngle();
            if (angle == null || e.getEventAt() == null) continue;

            String ageGroup = ageGroup(m.getBirthYear());
            String gender = m.getGender();
            String region = m.getRegion() != null ? m.getRegion() : "unknown";
            String period = e.getEventAt().toLocalDate().toString().substring(0, 7); // YYYY-MM
            String status = status(angle);

            PostureAcc pa = pmap.computeIfAbsent(
                    ageGroup + "|" + gender + "|" + region + "|" + period, k -> new PostureAcc());
            pa.members.add(mid);
            pa.sessions.add(e.getAnalysisId());
            pa.angleSum += angle;
            pa.count++;
            if ("good".equals(status)) pa.good++;
            else if ("watching".equals(status)) pa.watching++;
            else pa.alert++;

            LocalDateTime fs = firstSeen.get(mid);
            if (fs != null) {
                int week = (int) (Duration.between(fs, e.getEventAt()).toDays() / 7);
                if (week >= 0) {
                    CohortAcc ca = cmap.computeIfAbsent(
                            ageGroup + "|" + gender + "|" + week, k -> new CohortAcc());
                    ca.members.add(mid);
                    ca.angleSum += angle;
                    ca.count++;
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ── 세그먼트 자세 건강 UPSERT (전량 재계산) ──
        postureRepository.deleteAllInBatch();
        List<SegmentPostureStats> plist = new ArrayList<>();
        for (Map.Entry<String, PostureAcc> en : pmap.entrySet()) {
            String[] kv = en.getKey().split("\\|");
            PostureAcc pa = en.getValue();
            double tot = pa.count;
            SegmentPostureStats s = new SegmentPostureStats();
            s.setAgeGroup(kv[0]);
            s.setGender(kv[1]);
            s.setRegion(kv[2]);
            s.setPeriodType("monthly");
            s.setPeriodValue(kv[3]);
            s.setSampleSize(pa.members.size());
            s.setAvgNeckAngle(round1(pa.angleSum / tot));
            s.setTurtleNeckFreq(round2((double) pa.alert / Math.max(1, pa.sessions.size())));
            s.setStatusGood(round3(pa.good / tot));
            s.setStatusWatching(round3(pa.watching / tot));
            s.setStatusAlert(round3(pa.alert / tot));
            s.setUpdatedAt(now);
            plist.add(s);
        }
        postureRepository.saveAll(plist);

        // ── 코호트 UPSERT ──
        cohortRepository.deleteAllInBatch();
        List<SegmentCohortStats> clist = new ArrayList<>();
        for (Map.Entry<String, CohortAcc> en : cmap.entrySet()) {
            String[] kv = en.getKey().split("\\|");
            CohortAcc ca = en.getValue();
            SegmentCohortStats s = new SegmentCohortStats();
            s.setAgeGroup(kv[0]);
            s.setGender(kv[1]);
            s.setWeeksSinceStart(Integer.parseInt(kv[2]));
            s.setSampleSize(ca.members.size());
            s.setAvgNeckAngle(round1(ca.angleSum / ca.count));
            s.setUpdatedAt(now);
            clist.add(s);
        }
        cohortRepository.saveAll(clist);

        return plist.size() + clist.size();
    }

    private String ageGroup(Integer birthYear) {
        int age = LocalDate.now().getYear() - birthYear;
        if (age < 20) return "under20";
        if (age < 30) return "20s";
        if (age < 40) return "30s";
        if (age < 50) return "40s";
        return "50plus";
    }

    private String status(double angle) {
        if (angle < GOOD_MAX) return "good";
        if (angle < WATCH_MAX) return "watching";
        return "alert";
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    private static class PostureAcc {
        final Set<Long> members = new HashSet<>();
        final Set<Long> sessions = new HashSet<>();
        double angleSum = 0;
        int count = 0, good = 0, watching = 0, alert = 0;
    }

    private static class CohortAcc {
        final Set<Long> members = new HashSet<>();
        double angleSum = 0;
        int count = 0;
    }
}
