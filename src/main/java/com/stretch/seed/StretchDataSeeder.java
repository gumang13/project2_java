package com.stretch.seed;   // ← stretch 루트 밑. 실제 위치에 맞게

import com.stretch.constant.ExerciseCategory;
import com.stretch.entity.*;
import com.stretch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StretchDataSeeder implements ApplicationRunner {

    private final ExercisePoseRepository poseRepository;
    private final PoseCheckRepository poseCheckRepository;
    private final ExerciseRoutineRepository routineRepository;
    private final RoutinePoseRepository routinePoseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (poseRepository.count() > 0) return;        // 이미 있으면 전체 skip (멱등)

        Map<String, Long> poseIds = seedPoses();       // ① 자세 + 체크
        seedPresets(poseIds);                          // ② 프리셋
    }

    /* ───────── ① 자세 마스터 + 체크 항목 ───────── */
    private Map<String, Long> seedPoses() {
        Map<String, Long> ids = new LinkedHashMap<>();

        Long p1 = savePose("턱 당기기", "Chin Tuck", 10, false,
                "다음은 턱 당기기 자세입니다. 등을 펴고 옆모습이 화면에 보이도록 앉아주세요.");
        saveCheck(p1, 0, "귀-어깨 수직 정렬", "턱을 목 쪽으로 더 당겨주세요");
        saveCheck(p1, 1, "시선 정면 유지", "시선은 정면을 바라봐 주세요");
        ids.put("턱 당기기", p1);

        Long p2 = savePose("목 뒤로 젖히기", "Neck Extension", 8, false,
                "다음은 목 뒤로 젖히기입니다. 천천히 고개를 뒤로 젖혀주세요.");
        saveCheck(p2, 0, "머리 젖힘 각도", "고개를 더 젖혀 천장을 바라봐 주세요");
        saveCheck(p2, 1, "어깨 고정", "어깨는 그대로 두고 목만 움직여 주세요");
        ids.put("목 뒤로 젖히기", p2);

        Long p3 = savePose("앉아서 앞으로 숙이기", "Seated Forward Bend", 15, false,
                "다음은 앉아서 앞으로 숙이기입니다. 다리를 펴고 앉아주세요.");
        saveCheck(p3, 0, "상체 숙임", "상체를 더 앞으로 숙여주세요");
        saveCheck(p3, 1, "등 곧게 펴기", "등을 펴고 배부터 숙여주세요");
        ids.put("앉아서 앞으로 숙이기", p3);

        Long p4 = savePose("선 전굴", "Standing Forward Fold", 15, false,
                "다음은 선 전굴입니다. 일어서서 상체를 아래로 내려주세요.");
        saveCheck(p4, 0, "상체 내림", "상체를 더 아래로 내려주세요");
        saveCheck(p4, 1, "무릎 펴기", "무릎을 살짝만 펴주세요");
        ids.put("선 전굴", p4);

        Long p5 = savePose("로우 런지", "Low Lunge", 12, true,   // isBilateral = true (좌·우)
                "다음은 로우 런지입니다. 왼발을 앞으로 크게 내딛어 주세요.");
        saveCheck(p5, 0, "앞무릎 굽힘", "앞무릎을 더 굽혀주세요");
        saveCheck(p5, 1, "상체 세우기", "상체를 세워주세요");
        ids.put("로우 런지", p5);

        return ids;
    }

    private Long savePose(String nameKo, String nameEn, int holdSec,
                          boolean bilateral, String introTts) {
        ExercisePose p = new ExercisePose();
        p.setNameKo(nameKo);
        p.setNameEn(nameEn);
        p.setHoldSec(holdSec);
        p.setBilateral(bilateral);     // isBilateral 필드 → 세터는 setBilateral
        p.setIntroTts(introTts);
        return poseRepository.save(p).getId();
    }

    private void saveCheck(Long poseId, int order, String label, String coach) {
        PoseCheck c = new PoseCheck();
        c.setPoseId(poseId);
        c.setCheckOrder(order);
        c.setLabel(label);
        c.setCoachMessage(coach);
        c.setTargetMin(0f);            // TODO: 키포인트 판단 로직 만들 때 채우기 (임계 각도/거리)
        c.setTargetMax(0f);
        poseCheckRepository.save(c);
    }

    /* ───────── ② 프리셋 (시스템 루틴 = member_id null) ───────── */
    private void seedPresets(Map<String, Long> poseIds) {
        seedRoutine("빠른 모드", "자리에서 티 안 나게, 1~2분",
                ExerciseCategory.STRETCH, 1, 120,        // 난이도 1(초급), 120초
                List.of("턱 당기기", "목 뒤로 젖히기"), poseIds);

        seedRoutine("집중 모드", "제대로 풀어주는 전체 루틴, 약 5분",
                ExerciseCategory.STRETCH, 1, 300,        // 300초
                List.of("턱 당기기", "목 뒤로 젖히기", "앉아서 앞으로 숙이기",
                        "선 전굴", "로우 런지"), poseIds);
    }

    private void seedRoutine(String name, String desc, ExerciseCategory category,
                             int difficulty, int estSeconds,
                             List<String> poseNames, Map<String, Long> poseIds) {
        ExerciseRoutine routine = new ExerciseRoutine();
        routine.setMemberId(null);          // 시스템 프리셋 = 주인 없음
        routine.setCategory(category);
        routine.setName(name);
        routine.setDescription(desc);
        routine.setDifficulty(difficulty);
        routine.setEstSeconds(estSeconds);  // 초 단위
        Long routineId = routineRepository.save(routine).getId();

        int order = 0;
        for (String poseName : poseNames) {
            RoutinePose rp = new RoutinePose();
            rp.setRoutineId(routineId);
            rp.setPoseId(poseIds.get(poseName));
            rp.setPoseOrder(order++);
            routinePoseRepository.save(rp);
        }
    }
}