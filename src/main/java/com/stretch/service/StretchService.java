package com.stretch.service;

import com.stretch.dto.PoseResponse;
import com.stretch.dto.PresetResponse;
import com.stretch.dto.RecentSessionResponse;
import com.stretch.dto.StretchResponse;
import com.stretch.entity.ExercisePose;
import com.stretch.entity.ExerciseRoutine;
import com.stretch.entity.RoutinePose;
import com.stretch.repository.ExercisePoseRepository;
import com.stretch.repository.ExerciseRoutineRepository;
import com.stretch.repository.ExerciseSessionRepository;
import com.stretch.repository.RoutinePoseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StretchService {

    private final ExercisePoseRepository exercisePoseRepository;
    private final ExerciseSessionRepository exerciseSessionRepository;
    private final ExerciseRoutineRepository exerciseRoutineRepository;
    private final RoutinePoseRepository routinePoseRepository;

    @Transactional(readOnly = true)
    public StretchResponse getStretchPage(Long memberId) {
        List<PresetResponse> presets = exerciseRoutineRepository.findByMemberIdIsNull()
                .stream()
                .map(this::toPresetResponse)
                .toList();

        List<RecentSessionResponse> recentSessions = getRecentSessions(memberId);

        return StretchResponse.builder()
                .presets(presets)
                .recentSessions(recentSessions)
                .build();
    }

    /* 루틴 1개 → 담긴 자세까지 엮어서 PresetResponse 로 */
    private PresetResponse toPresetResponse(ExerciseRoutine routine) {
        List<RoutinePose> links = routinePoseRepository
                .findByRoutineIdOrderByPoseOrder(routine.getId());

        List<Long> poseIds = links.stream().map(RoutinePose::getPoseId).toList();
        Map<Long, ExercisePose> poseMap = exercisePoseRepository.findAllById(poseIds)
                .stream()
                .collect(Collectors.toMap(ExercisePose::getId, p -> p));

        List<PoseResponse> poses = links.stream()
                .map(link -> PoseResponse.from(poseMap.get(link.getPoseId())))
                .toList();

        return PresetResponse.builder()
                .routineId(routine.getId())
                .name(routine.getName())
                .description(routine.getDescription())
                .estSeconds(routine.getEstSeconds())
                .poses(poses)
                .build();
    }

    /* 최근 세션 5개 → Response (루틴 이름 채워서) */
    private List<RecentSessionResponse> getRecentSessions(Long memberId) {
        return exerciseSessionRepository
                .findTop5ByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(s -> {
                    String routineName = exerciseRoutineRepository.findById(s.getRoutineId())
                            .map(ExerciseRoutine::getName)
                            .orElse("(삭제된 루틴)");
                    return RecentSessionResponse.from(s, routineName);
                })
                .toList();
    }

}
