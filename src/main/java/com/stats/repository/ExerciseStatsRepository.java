package com.stats.repository;

import com.stretch.constant.SessionStatus;
import com.stretch.entity.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExerciseStatsRepository extends JpaRepository<ExerciseSession, Long> {

    // 통계 탭: 기간 내 운동 세션을 오래된 순으로 조회합니다.
    List<ExerciseSession> findByMemberIdAndStartedAtBetweenOrderByStartedAtAsc(
            Long memberId,
            LocalDateTime from,
            LocalDateTime to
    );

    // 통계 탭: 최근 운동 세션 로그를 최신순으로 조회합니다.
    List<ExerciseSession> findTop5ByMemberIdAndStartedAtBetweenOrderByStartedAtDesc(
            Long memberId,
            LocalDateTime from,
            LocalDateTime to
    );

    // 연속 운동일(streak)은 요청 기간(day/week/month)과 상관없이 회원의 전체 완료 세션을 최신순 보고 계산
    List<ExerciseSession> findByMemberIdAndStatusOrderByStartedAtDesc(
            Long memberId,
            SessionStatus status
    );

    List<ExerciseSession> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
