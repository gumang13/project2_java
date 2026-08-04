package com.stretch.seed;

import com.stretch.constant.ExerciseCategory;
import com.stretch.entity.ExercisePose;
import com.stretch.entity.ExerciseRoutine;
import com.stretch.entity.RoutinePose;
import com.stretch.repository.ExercisePoseRepository;
import com.stretch.repository.ExerciseRoutineRepository;
import com.stretch.repository.RoutinePoseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(20)
@RequiredArgsConstructor
@Profile("!test")
public class TrainedPoseSeeder implements ApplicationRunner {

    private static final List<String> FOCUS_POSE_KEYS = List.of(
            "mountain_pose",
            "warrior_ii_pose",
            "triangle_pose",
            "tree_pose",
            "eagle_pose"
    );

    private static final List<String> QUICK_POSE_KEYS = List.of(
            "side_neck_stretch",
            "chest_opener",
            "triceps_stretch",
            "standing_side_bend",
            "eagle_arms"
    );

    private final ExercisePoseRepository poseRepository;
    private final ExerciseRoutineRepository routineRepository;
    private final RoutinePoseRepository routinePoseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SeedPose> poses = List.of(
                new SeedPose("mountain_pose", "산 자세", "Mountain Pose", 12, false),
                new SeedPose("warrior_ii_pose", "전사 2 자세", "Warrior II Pose", 12, false),
                new SeedPose("triangle_pose", "삼각 자세", "Triangle Pose", 12, false),
                new SeedPose("tree_pose", "나무 자세", "Tree Pose", 12, false),
                new SeedPose("eagle_pose", "독수리 자세", "Eagle Pose", 12, false),
                new SeedPose("side_neck_stretch", "목 옆 늘리기", "Side Neck Stretch", 10, false),
                new SeedPose("chest_opener", "가슴 열기", "Chest Opener", 10, false),
                new SeedPose("triceps_stretch", "삼두근 늘리기", "Triceps Stretch", 10, false),
                new SeedPose("standing_side_bend", "측면 늘리기", "Standing Side Bend", 10, false),
                new SeedPose("eagle_arms", "독수리 팔", "Eagle Arms", 10, false)
        );

        for (SeedPose seed : poses) {
            ExercisePose pose = poseRepository.findByPoseKey(seed.poseKey()).orElseGet(ExercisePose::new);
            pose.setPoseKey(seed.poseKey());
            pose.setNameKo(seed.nameKo());
            pose.setNameEn(seed.nameEn());
            pose.setHoldSec(seed.holdSec());
            pose.setBilateral(seed.bilateral());
            pose.setIntroTts(seed.nameKo() + " 자세를 준비해 주세요.");
            poseRepository.save(pose);
        }

        Map<String, ExercisePose> poseMap = poseRepository.findByPoseKeyIn(
                poses.stream().map(SeedPose::poseKey).toList()
        ).stream().collect(Collectors.toMap(ExercisePose::getPoseKey, Function.identity()));

        syncSystemRoutine("빠른 모드", "상반신 위주 5개를 빠르게 이어서 하는 루틴이에요.", ExerciseCategory.STRETCH, QUICK_POSE_KEYS, poseMap);
        syncSystemRoutine("집중 모드", "전신 5개를 차분하게 이어서 하는 루틴이에요.", ExerciseCategory.YOGA, FOCUS_POSE_KEYS, poseMap);

        poseRepository.findAll().stream()
                .filter(pose -> pose.getPoseKey() == null || pose.getPoseKey().isBlank())
                .forEach(poseRepository::delete);
    }

    private void syncSystemRoutine(
            String name,
            String description,
            ExerciseCategory category,
            List<String> poseKeys,
            Map<String, ExercisePose> poseMap
    ) {
        ExerciseRoutine routine = routineRepository.findByMemberIdIsNull().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(ExerciseRoutine::new);

        routine.setMemberId(null);
        routine.setName(name);
        routine.setDescription(description);
        routine.setCategory(category);
        routine.setDifficulty(1);
        routine.setEstSeconds(poseKeys.stream().map(poseMap::get).mapToInt(ExercisePose::getHoldSec).sum());

        ExerciseRoutine saved = routineRepository.save(routine);
        routinePoseRepository.deleteByRoutineId(saved.getId());
        for (int i = 0; i < poseKeys.size(); i++) {
            RoutinePose link = new RoutinePose();
            link.setRoutineId(saved.getId());
            link.setPoseId(poseMap.get(poseKeys.get(i)).getId());
            link.setPoseOrder(i + 1);
            routinePoseRepository.save(link);
        }
    }

    private record SeedPose(String poseKey, String nameKo, String nameEn, int holdSec, boolean bilateral) {
    }
}