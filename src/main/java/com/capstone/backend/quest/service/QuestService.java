package com.capstone.backend.quest.service;

import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.quest.dto.CompleteQuestRequest;
import com.capstone.backend.quest.dto.QuestExerciseResponse;
import com.capstone.backend.quest.dto.QuestResponse;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.quest.service.HealthQuestProgressEvaluator.HealthQuestProgress;
import com.capstone.backend.reward.policy.QuestRewardPolicy;
import com.capstone.backend.reward.policy.QuestRewardPolicy.QuestReward;
import com.capstone.backend.reward.service.RewardService;
import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.routine.entity.RoutineSession;
import com.capstone.backend.routine.repository.RoutineRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestService {

    private static final BigDecimal RECOVERY_THRESHOLD = BigDecimal.valueOf(0.80);
    private static final BigDecimal REDUCE_THRESHOLD = BigDecimal.valueOf(1.00);

    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final RoutineRepository routineRepository;
    private final RewardService rewardService;
    private final HealthQuestProgressEvaluator healthQuestProgressEvaluator;

    public QuestService(UserQuestRepository userQuestRepository,
                        UserRepository userRepository,
                        ConditionLogRepository conditionLogRepository,
                        RoutineRepository routineRepository,
                        RewardService rewardService,
                        HealthQuestProgressEvaluator healthQuestProgressEvaluator) {
        this.userQuestRepository = userQuestRepository;
        this.userRepository = userRepository;
        this.conditionLogRepository = conditionLogRepository;
        this.routineRepository = routineRepository;
        this.rewardService = rewardService;
        this.healthQuestProgressEvaluator = healthQuestProgressEvaluator;
    }

    @Transactional
    public QuestResponse getTodayQuest(Long userId) {
        LocalDate today = KoreanTime.today();
        userQuestRepository.expireUnfinishedBefore(userId, today);

        return userQuestRepository.findByUser_IdAndQuestDate(userId, today)
                .map(this::returnExistingTodayQuest)
                .orElseGet(() -> createTodayQuest(userId, today));
    }

    @Transactional
    public QuestResponse completeQuest(Long userId, Long questId, CompleteQuestRequest request) {
        UserQuest quest = userQuestRepository.findByIdAndUser_Id(questId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QUEST_NOT_FOUND", "퀘스트를 찾을 수 없습니다."));

        LocalDate today = KoreanTime.today();
        if (quest.getQuestDate().isBefore(today)) {
            quest.expire();
            throw new ApiException(HttpStatus.CONFLICT, "QUEST_EXPIRED", "지난 날짜의 미완료 퀘스트는 완료할 수 없습니다.");
        }
        if (UserQuest.STATUS_EXPIRED.equals(quest.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "QUEST_EXPIRED", "만료된 퀘스트는 완료할 수 없습니다.");
        }
        boolean newlyCompleted = !UserQuest.STATUS_COMPLETED.equals(quest.getStatus());
        if (newlyCompleted) {
            HealthQuestProgress healthProgress = request == null ? null : healthQuestProgressEvaluator.evaluate(quest, request.healthSamples());
            Integer progressValue = healthProgress != null && healthProgress.progressValue() != null
                    ? healthProgress.progressValue()
                    : request == null ? null : request.progressValue();
            Map<String, Object> proof = healthProgress != null
                    ? healthProgress.proof()
                    : request == null ? null : request.proof();
            quest.complete(progressValue, proof);
            rewardService.issueQuestCompletionRewards(quest);
        }
        return QuestResponse.from(quest, exercisesFromContext(quest));
    }

    private QuestResponse returnExistingTodayQuest(UserQuest quest) {
        quest.markIssued();
        return QuestResponse.from(quest, exercisesFromContext(quest));
    }

    private QuestResponse createTodayQuest(Long userId, LocalDate today) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        if (!user.isOnboardingCompleted()) {
            throw new ApiException(HttpStatus.CONFLICT, "ONBOARDING_REQUIRED", "퀘스트를 생성하려면 온보딩 프로필을 먼저 저장해 주세요.");
        }

        ConditionLog conditionLog = conditionLogRepository.findByUser_IdAndLogDate(userId, today)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "CONDITION_REQUIRED", "오늘 퀘스트를 생성하려면 먼저 오늘 컨디션을 입력해 주세요."));

        Routine activeRoutine = user.getActiveRoutine();
        if (activeRoutine == null) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTIVE_ROUTINE_REQUIRED", "오늘 퀘스트를 생성하려면 추천 루틴을 선택하거나 내 루틴을 먼저 만들어 주세요.");
        }

        Routine routine = routineRepository.findWithSessionsByIdAndActiveTrue(activeRoutine.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "ACTIVE_ROUTINE_REQUIRED", "활성 루틴을 찾을 수 없습니다. 다른 루틴을 다시 활성화해 주세요."));

        UserQuest quest = buildQuest(user, routine, conditionLog, today);
        UserQuest savedQuest = userQuestRepository.save(quest);
        return QuestResponse.from(savedQuest, exercisesFromContext(savedQuest));
    }

    private UserQuest buildQuest(User user, Routine routine, ConditionLog conditionLog, LocalDate today) {
        RoutineSession todaySession = findTodaySession(routine, today);
        BigDecimal multiplier = conditionLog.getExerciseMultiplier();
        if (multiplier != null && multiplier.compareTo(RECOVERY_THRESHOLD) < 0) {
            return recoveryQuest(user, routine, todaySession, conditionLog, today);
        }
        if (todaySession == null) {
            return offDayQuest(user, routine, conditionLog, today);
        }
        return routineQuest(user, routine, todaySession, conditionLog, today);
    }

    private RoutineSession findTodaySession(Routine routine, LocalDate today) {
        String dayOfWeek = today.getDayOfWeek().name();
        return routine.getSessions().stream()
                .filter(session -> Boolean.TRUE.equals(session.getActive()))
                .filter(session -> dayOfWeek.equalsIgnoreCase(session.getDayOfWeek()))
                .min(Comparator.comparing(RoutineSession::getSeq, Comparator.nullsLast(Integer::compareTo)))
                .orElse(null);
    }

    private UserQuest routineQuest(User user,
                                   Routine routine,
                                   RoutineSession session,
                                   ConditionLog conditionLog,
                                   LocalDate today) {
        List<RoutineItem> allItems = session.getItems().stream()
                .sorted(Comparator.comparing(RoutineItem::getSeq, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (allItems.isEmpty()) {
            return offDayQuest(user, routine, conditionLog, today);
        }

        Map<String, Object> context = baseContext(routine, session, conditionLog);
        context.put("exercises", allItems.stream().map(this::exerciseContext).toList());

        String title = session.getSessionName() + " 루틴 완료";
        String description = session.getSessionName() + " 세션의 운동 루틴을 완료해 주세요. 포함된 운동은 총 "
                + allItems.size() + "개입니다.";
        QuestReward reward = QuestRewardPolicy.routine(session.getEstimatedMinutes());

        return UserQuest.create(
                user,
                routine,
                session,
                conditionLog,
                today,
                UserQuest.TYPE_ROUTINE,
                UserQuest.METRIC_ROUTINE,
                title,
                description,
                1,
                false,
                reward.currency(),
                reward.exp(),
                context
        );
    }

    private UserQuest offDayQuest(User user, Routine routine, ConditionLog conditionLog, LocalDate today) {
        int targetMinutes = conditionLog.getExerciseMultiplier().compareTo(REDUCE_THRESHOLD) < 0 ? 10 : 15;
        boolean conditionAdjusted = targetMinutes < 15;
        Map<String, Object> context = baseContext(routine, null, conditionLog);
        context.put("recommendedAction", "걷기 또는 스트레칭");
        QuestReward reward = QuestRewardPolicy.offDay(targetMinutes);

        return UserQuest.create(
                user,
                routine,
                null,
                conditionLog,
                today,
                UserQuest.TYPE_OFF_DAY,
                UserQuest.METRIC_MINUTES,
                "오프데이 회복 운동 " + targetMinutes + "분",
                "오늘은 루틴이 없는 날입니다. 가볍게 걷기 또는 스트레칭을 " + targetMinutes + "분 진행해 주세요.",
                targetMinutes,
                conditionAdjusted,
                reward.currency(),
                reward.exp(),
                context
        );
    }

    private UserQuest recoveryQuest(User user,
                                    Routine routine,
                                    RoutineSession sourceSession,
                                    ConditionLog conditionLog,
                                    LocalDate today) {
        Map<String, Object> context = baseContext(routine, sourceSession, conditionLog);
        context.put("recommendedAction", "가벼운 스트레칭");
        QuestReward reward = QuestRewardPolicy.recovery();

        return UserQuest.create(
                user,
                routine,
                sourceSession,
                conditionLog,
                today,
                UserQuest.TYPE_RECOVERY,
                UserQuest.METRIC_MINUTES,
                "컨디션 회복 스트레칭 10분",
                "오늘 컨디션이 낮아 루틴 운동 대신 가벼운 스트레칭 10분으로 조정했습니다.",
                10,
                true,
                reward.currency(),
                reward.exp(),
                context
        );
    }

    private Map<String, Object> baseContext(Routine routine, RoutineSession session, ConditionLog conditionLog) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("routineId", routine.getId());
        context.put("routineName", routine.getName());
        if (session != null) {
            context.put("sourceSessionId", session.getId());
            context.put("sessionName", session.getSessionName());
            context.put("sessionType", session.getSessionType());
            context.put("dayOfWeek", session.getDayOfWeek());
        }
        context.put("conditionLogId", conditionLog.getId());
        context.put("conditionScore", conditionLog.getConditionScore());
        context.put("exerciseMultiplier", conditionLog.getExerciseMultiplier());
        return context;
    }

    private Map<String, Object> exerciseContext(RoutineItem item) {
        Exercise exercise = item.getExercise();
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("exerciseId", exercise.getId());
        context.put("exerciseName", exercise.getName());
        context.put("category", exercise.getCategory());
        context.put("seq", item.getSeq());
        context.put("targetSets", item.getSets());
        context.put("targetReps", item.getReps());
        context.put("targetDurationSec", item.getDurationSec());
        return context;
    }

    private List<QuestExerciseResponse> exercisesFromContext(UserQuest quest) {
        Object rawExercises = quest.getQuestContextJson().get("exercises");
        if (!(rawExercises instanceof List<?> exercises)) {
            return List.of();
        }

        List<QuestExerciseResponse> responses = new ArrayList<>();
        for (Object rawExercise : exercises) {
            if (rawExercise instanceof Map<?, ?> exercise) {
                responses.add(new QuestExerciseResponse(
                        longValue(exercise.get("exerciseId")),
                        stringValue(exercise.get("exerciseName")),
                        stringValue(exercise.get("category")),
                        intValue(exercise.get("seq")),
                        intValue(exercise.get("targetSets")),
                        intValue(exercise.get("targetReps")),
                        intValue(exercise.get("targetDurationSec"))
                ));
            }
        }
        return responses;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Long.valueOf(string);
        }
        return null;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Integer.valueOf(string);
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
