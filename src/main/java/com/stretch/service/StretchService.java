package com.stretch.service;

import com.stretch.constant.ExerciseCategory;
import com.stretch.constant.SessionStatus;
import com.stretch.dto.PoseResponse;
import com.stretch.dto.PresetResponse;
import com.stretch.dto.RecentSessionResponse;
import com.stretch.dto.RoutineSaveRequest;
import com.stretch.dto.StretchPoseCompleteRequest;
import com.stretch.dto.StretchPoseCompleteResponse;
import com.stretch.dto.StretchResponse;
import com.stretch.dto.StretchSessionStartRequest;
import com.stretch.dto.StretchSessionStartResponse;
import com.stretch.entity.ExercisePose;
import com.stretch.entity.ExerciseRoutine;
import com.stretch.entity.ExerciseSession;
import com.stretch.entity.PoseResult;
import com.stretch.entity.RoutinePose;
import com.stretch.repository.ExercisePoseRepository;
import com.stretch.repository.ExerciseRoutineRepository;
import com.stretch.repository.ExerciseSessionRepository;
import com.stretch.repository.PoseResultRepository;
import com.stretch.repository.RoutinePoseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StretchService {

    private final ExercisePoseRepository exercisePoseRepository;
    private final ExerciseSessionRepository exerciseSessionRepository;
    private final ExerciseRoutineRepository exerciseRoutineRepository;
    private final RoutinePoseRepository routinePoseRepository;
    private final PoseResultRepository poseResultRepository;

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

    @Transactional(readOnly = true)
    public List<PresetResponse> getMyRoutines(Long memberId) {
        return exerciseRoutineRepository.findByMemberIdOrderByUpdatedAtDesc(memberId)
                .stream()
                .map(this::toPresetResponse)
                .toList();
    }

    @Transactional
    public PresetResponse saveMyRoutine(Long memberId, RoutineSaveRequest request) {
        List<ExercisePose> poses = resolvePoses(request);
        if (poses.isEmpty()) {
            throw new IllegalArgumentException("At least one valid pose is required.");
        }

        ExerciseRoutine routine = resolveRoutineForSave(memberId, request.getRoutineId());
        routine.setMemberId(memberId);
        routine.setCategory(request.getCategory() == null ? ExerciseCategory.STRETCH : request.getCategory());
        routine.setName(request.getName().trim());
        routine.setDescription(request.getDescription());
        routine.setDifficulty(request.getDifficulty());
        routine.setEstSeconds(poses.stream().mapToInt(ExercisePose::getHoldSec).sum());

        ExerciseRoutine saved = exerciseRoutineRepository.save(routine);
        routinePoseRepository.deleteByRoutineId(saved.getId());
        for (int i = 0; i < poses.size(); i++) {
            RoutinePose link = new RoutinePose();
            link.setRoutineId(saved.getId());
            link.setPoseId(poses.get(i).getId());
            link.setPoseOrder(i + 1);
            routinePoseRepository.save(link);
        }

        return toPresetResponse(saved);
    }

    @Transactional
    public StretchSessionStartResponse startSession(Long memberId, StretchSessionStartRequest request) {
        List<ExercisePose> poses = resolvePoses(request.getPoseKeys());
        if (poses.isEmpty()) {
            throw new IllegalArgumentException("At least one valid pose is required.");
        }

        Long routineId = resolveRoutineId(request);
        ExerciseSession session = new ExerciseSession();
        session.setMemberId(memberId);
        session.setRoutineId(routineId);
        session.setStartedAt(LocalDateTime.now());
        session.setTotalPoses(poses.size());
        session.setCompletedPoses(0);

        ExerciseSession saved = exerciseSessionRepository.save(session);
        return StretchSessionStartResponse.builder()
                .sessionId(saved.getId())
                .routineId(routineId)
                .totalPoses(saved.getTotalPoses())
                .build();
    }

    @Transactional
    public StretchPoseCompleteResponse completePose(Long memberId, Long sessionId, StretchPoseCompleteRequest request) {
        ExerciseSession session = exerciseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found."));
        if (!Objects.equals(session.getMemberId(), memberId)) {
            throw new IllegalArgumentException("Cannot update another member's session.");
        }

        int safeOrder = Math.max(1, request.getResultOrder());
        poseResultRepository.findBySessionIdAndResultOrder(sessionId, safeOrder)
                .orElseGet(() -> poseResultRepository.save(buildPoseResult(sessionId, safeOrder, request)));

        int completedCount = poseResultRepository.countBySessionId(sessionId);
        session.setCompletedPoses(Math.min(completedCount, session.getTotalPoses()));
        if (session.getCompletedPoses() >= session.getTotalPoses()) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndedAt(LocalDateTime.now());
        }

        return StretchPoseCompleteResponse.builder()
                .sessionId(session.getId())
                .completedPoses(session.getCompletedPoses())
                .totalPoses(session.getTotalPoses())
                .sessionCompleted(session.getStatus() == SessionStatus.COMPLETED)
                .build();
    }

    private PoseResult buildPoseResult(Long sessionId, int resultOrder, StretchPoseCompleteRequest request) {
        ExercisePose pose = exercisePoseRepository.findByPoseKey(request.getPoseKey())
                .orElseThrow(() -> new IllegalArgumentException("Pose not found."));

        PoseResult result = new PoseResult();
        result.setSessionId(sessionId);
        result.setPoseId(pose.getId());
        result.setResultOrder(resultOrder);
        result.setCompleted(request.isCompleted());
        result.setCoachCount(Math.max(0, request.getCoachCount()));
        result.setHoldAchievedSec(Math.max(0, request.getHoldAchievedSec()));
        return result;
    }

    private Long resolveRoutineId(StretchSessionStartRequest request) {
        if (request.getRoutineId() != null) {
            return request.getRoutineId();
        }

        String source = request.getSource();
        if ("quick".equals(source)) {
            return findSystemRoutineId("빠른 모드");
        }
        if ("focus".equals(source)) {
            return findSystemRoutineId("집중 모드");
        }
        return null;
    }

    private Long findSystemRoutineId(String name) {
        return exerciseRoutineRepository.findByMemberIdIsNull().stream()
                .filter(routine -> name.equals(routine.getName()))
                .map(ExerciseRoutine::getId)
                .findFirst()
                .orElse(null);
    }

    private List<ExercisePose> resolvePoses(RoutineSaveRequest request) {
        List<ExercisePose> poses = resolvePoses(request.getPoseKeys());
        if (!poses.isEmpty()) {
            return poses;
        }

        List<Long> poseIds = normalizePoseIds(request.getPoseIds());
        Map<Long, ExercisePose> poseMap = exercisePoseRepository.findAllById(poseIds)
                .stream()
                .collect(Collectors.toMap(ExercisePose::getId, p -> p));
        if (poseMap.size() != poseIds.size()) {
            throw new IllegalArgumentException("Some poses do not exist.");
        }
        return poseIds.stream().map(poseMap::get).toList();
    }

    private List<ExercisePose> resolvePoses(List<String> requestedPoseKeys) {
        List<String> poseKeys = normalizePoseKeys(requestedPoseKeys);
        if (poseKeys.isEmpty()) {
            return List.of();
        }

        Map<String, ExercisePose> poseMap = exercisePoseRepository.findByPoseKeyIn(poseKeys)
                .stream()
                .collect(Collectors.toMap(ExercisePose::getPoseKey, p -> p));
        if (poseMap.size() != poseKeys.size()) {
            throw new IllegalArgumentException("Some pose keys do not exist.");
        }
        return poseKeys.stream().map(poseMap::get).toList();
    }

    private ExerciseRoutine resolveRoutineForSave(Long memberId, Long routineId) {
        if (routineId == null) {
            return new ExerciseRoutine();
        }

        ExerciseRoutine routine = exerciseRoutineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found."));
        if (!Objects.equals(routine.getMemberId(), memberId)) {
            throw new IllegalArgumentException("Cannot update another member's routine.");
        }
        return routine;
    }

    private List<String> normalizePoseKeys(List<String> poseKeys) {
        Set<String> unique = new LinkedHashSet<>();
        if (poseKeys != null) {
            for (String poseKey : poseKeys) {
                if (poseKey != null && !poseKey.isBlank()) {
                    unique.add(poseKey.trim());
                }
            }
        }
        return unique.stream().toList();
    }

    private List<Long> normalizePoseIds(List<Long> poseIds) {
        Set<Long> unique = new LinkedHashSet<>(poseIds == null ? List.of() : poseIds);
        return unique.stream().filter(id -> id != null && id > 0).toList();
    }

    private PresetResponse toPresetResponse(ExerciseRoutine routine) {
        List<RoutinePose> links = routinePoseRepository
                .findByRoutineIdOrderByPoseOrder(routine.getId());

        List<Long> poseIds = links.stream().map(RoutinePose::getPoseId).toList();
        Map<Long, ExercisePose> poseMap = exercisePoseRepository.findAllById(poseIds)
                .stream()
                .collect(Collectors.toMap(ExercisePose::getId, p -> p, (a, b) -> a, LinkedHashMap::new));

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

    private List<RecentSessionResponse> getRecentSessions(Long memberId) {
        return exerciseSessionRepository
                .findTop5ByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(s -> {
                    String routineName = exerciseRoutineRepository.findById(s.getRoutineId())
                            .map(ExerciseRoutine::getName)
                            .orElse("Unknown routine");
                    return RecentSessionResponse.from(s, routineName);
                })
                .toList();
    }
}