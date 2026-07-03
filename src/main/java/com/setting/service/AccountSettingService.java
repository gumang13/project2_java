package com.setting.service;

import com.analysis.repository.AnalysisEventRepository;
import com.analysis.repository.AnalysisRepository;
import com.member.entity.Member;
import com.member.repository.MemberRepository;
import com.setting.dto.AccountInfoResponse;
import com.setting.dto.AccountUpdateRequest;
import com.setting.repository.SettingCalibrationRepository;
import com.setting.repository.SettingPaymentMethodRepository;
import com.setting.repository.SettingPaymentRepository;
import com.setting.repository.SettingRepository;
import com.setting.repository.SettingSubscriptionRepository;
import com.stats.repository.DailyStatsRepository;
import com.stats.repository.ExercisePoseResultStatsRepository;
import com.stats.repository.ExerciseStatsRepository;
import com.stretch.entity.ExerciseSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountSettingService {

    // 계정관리 파트는 Member를 새로 만들지 않고,
    // 이미 만들어진 MemberRepository를 가져와서 조회/수정만 담당합니다.
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingCalibrationRepository settingCalibrationRepository;
    private final AnalysisRepository analysisRepository;
    private final AnalysisEventRepository analysisEventRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final SettingRepository settingRepository;
    private final SettingSubscriptionRepository settingSubscriptionRepository;
    private final SettingPaymentRepository settingPaymentRepository;
    private final SettingPaymentMethodRepository settingPaymentMethodRepository;
    private final ExerciseStatsRepository exerciseStatsRepository;
    private final ExercisePoseResultStatsRepository exercisePoseResultStatsRepository;


    // memberId로 회원을 찾아 계정관리 화면에 보여줄 정보를 만듭니다.
    public AccountInfoResponse getAccountInfo(Long memberId) {
        Member member = findMember(memberId);
        return createAccountInfoResponse(member);
    }

    @Transactional
    public AccountInfoResponse updateName(Long memberId, AccountUpdateRequest request) {
        Member member = findMember(memberId);
        String name = normalizeName(request.name());

        member.setName(name);
        return createAccountInfoResponse(member);
    }

    @Transactional
    public AccountInfoResponse updateEmail(Long memberId, AccountUpdateRequest request) {
        Member member = findMember(memberId);
        String email = normalizeEmail(request.email());

        // 변경하려는 이메일을 다른 회원이 이미 사용 중이면 저장하지 않습니다.
        // 단, 자기 자신의 기존 이메일로 요청한 경우는 허용합니다.
        memberRepository.findByEmail(email)
                .filter(existingMember -> !existingMember.getId().equals(memberId))
                .ifPresent(existingMember -> {
                    throw new IllegalArgumentException();
                });

        // JPA 영속성 컨텍스트 안에서 값만 바꿔도 트랜잭션 종료 시 DB에 반영됩니다.
        member.setEmail(email);
        return createAccountInfoResponse(member);
    }

    @Transactional
    public Boolean updatePassword(Long memberId, AccountUpdateRequest request) {
        Member member = findMember(memberId);

        if (isBlank(request.password()) || isBlank(request.newPassword())) {
            throw new IllegalArgumentException();
        }

        // DB에는 비밀번호 원문이 아니라 암호화된 해시가 저장되어 있으므로
        // PasswordEncoder.matches()로 현재 비밀번호가 맞는지 확인합니다.
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException();
        }

        // 새 비밀번호도 반드시 암호화해서 저장합니다.
        member.setPassword(passwordEncoder.encode(request.newPassword()));
        return true;
    }

    // 계정관리 화면에 필요한 추가값을 모아서 응답 DTO를 만듭니다.
    private AccountInfoResponse createAccountInfoResponse(Member member) {
        // TODO: 통계 저장 로직이 연결되면 실제 이번 주 평균 점수로 교체합니다.
        Double weeklyAverageScore = 0.0;

        // TODO: DailyStatsRepository가 연결되면 이번 주 알림 횟수 합계로 교체합니다.
        Integer weeklyAlertCount =500;

        // 가입일 기준으로 사용 기간을 계산합니다.
        Integer usageDays = calculateUsageDays(member.getCreatedDate());

        // 캘리브레이션 데이터가 있으면 완료 상태로 표시합니다.
        Boolean calibrationCompleted =
                settingCalibrationRepository.existsByMemberId(member.getId());

        return AccountInfoResponse.from(
                member,
                weeklyAverageScore,
                weeklyAlertCount,
                usageDays,
                calibrationCompleted
        );
    }

    // createdDate부터 오늘까지의 사용 일수를 계산합니다.
    private Integer calculateUsageDays(LocalDateTime createdDate) {
        if (createdDate == null) {
            return 0;
        }

        return (int) ChronoUnit.DAYS.between(
                createdDate.toLocalDate(),
                LocalDate.now()
        ) + 1;
    }

    // 계정관리 API들은 모두 memberId 기준으로 회원을 먼저 찾아야 합니다.
    // 존재하지 않는 memberId면 예외를 발생시킵니다.
    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException());
    }

    // 이름 수정 전에 빈 값을 검사하고 앞뒤 공백을 제거합니다.
    private String normalizeName(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException();
        }

        return name.trim();
    }

    // 이메일 수정 전에 빈 값과 기본 형식을 검사합니다.
    // 복잡한 이메일 정규식 대신, 화면 설계서 수준에 맞춰 @ 포함 여부만 확인합니다.
    private String normalizeEmail(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException();
        }

        String normalizedEmail = email.trim();
        if (!normalizedEmail.contains("@")) {
            throw new IllegalArgumentException();
        }

        return normalizedEmail;
    }

    // null, 빈 문자열, 공백 문자열을 모두 빈 값으로 처리합니다.
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    //DB 작업을 하나로 묶음처리 멤버 id 조회해서 삭제
    @Transactional
    public void resetStats(Long memberId) {
        findMember(memberId);
        deleteStats(memberId);
    }
    //위에 deletStats 연결 - 이 회원이 가진 분석기록 조회 후 비어있지 않으면 삭제
    private void deleteStats(Long memberId) {
        List<Long> analysisIds = analysisRepository.findIdsByMemberId(memberId);
        List<Long> exerciseSessionIds = exerciseStatsRepository.findByMemberId(memberId)
                .stream()
                .map(ExerciseSession::getId)
                .toList();

        if (!analysisIds.isEmpty()) {
            analysisEventRepository.deleteByAnalysisIdIn(analysisIds);
        }
        analysisRepository.deleteByMemberId(memberId);
        dailyStatsRepository.deleteByMemberId(memberId);

        if (!exerciseSessionIds.isEmpty()) {
            exercisePoseResultStatsRepository.deleteBySessionIdIn(exerciseSessionIds);
        }
        exerciseStatsRepository.deleteByMemberId(memberId);

    }
    //계정 탈퇴(삭제) 진행 memberId랑 이어진 데이터들 삭제(통계데이터초기화(삭제) 메서드도 위에서 가져와서넣음)
    @Transactional
    public void withdraw(Long memberId) {
        Member member = findMember(memberId);

        deleteStats(memberId);
        settingPaymentRepository.deleteByMemberId(memberId);
        settingPaymentMethodRepository.deleteByMemberId(memberId);
        settingSubscriptionRepository.deleteByMemberId(memberId);
        settingCalibrationRepository.deleteByMemberId(memberId);
        settingRepository.deleteByUserId(memberId);
        memberRepository.delete(member);
    }

}
