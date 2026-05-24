package com.capstone.backend.quest.service;

import com.capstone.backend.achievement.service.AchievementBadgeService;
import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.exercise.repository.ExerciseRepository;
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
import com.capstone.backend.shop.service.ShopPassEffectService;
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
    private static final int MANUAL_COMPLETION_REWARD_EXP = 10;
    private static final int MANUAL_COMPLETION_REWARD_CURRENCY = 5;
    private static final String COMPLETION_TYPE_VERIFIED = "VERIFIED";
    private static final String COMPLETION_TYPE_MANUAL = "MANUAL";
    private static final String VERIFICATION_STATUS_VERIFIED = "VERIFIED";
    private static final String VERIFICATION_STATUS_NOT_PROVIDED = "NOT_PROVIDED";
    private static final String VERIFICATION_STATUS_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final RoutineRepository routineRepository;
    private final RewardService rewardService;
    private final HealthQuestProgressEvaluator healthQuestProgressEvaluator;
    private final ShopPassEffectService shopPassEffectService;
    private final AchievementBadgeService achievementBadgeService;
    private final ExerciseRepository exerciseRepository;

    public QuestService(UserQuestRepository userQuestRepository,
                        UserRepository userRepository,
                        ConditionLogRepository conditionLogRepository,
                        RoutineRepository routineRepository,
                        RewardService rewardService,
                        HealthQuestProgressEvaluator healthQuestProgressEvaluator,
                        ShopPassEffectService shopPassEffectService,
                        AchievementBadgeService achievementBadgeService,
                        ExerciseRepository exerciseRepository) {
        this.userQuestRepository = userQuestRepository;
        this.userRepository = userRepository;
        this.conditionLogRepository = conditionLogRepository;
        this.routineRepository = routineRepository;
        this.rewardService = rewardService;
        this.healthQuestProgressEvaluator = healthQuestProgressEvaluator;
        this.shopPassEffectService = shopPassEffectService;
        this.achievementBadgeService = achievementBadgeService;
        this.exerciseRepository = exerciseRepository;
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
            HealthQuestProgress healthProgress = evaluateHealthProgress(quest, request);
            QuestCompletion completion = completionFor(quest, request, healthProgress);
            Integer progressValue = healthProgress != null && healthProgress.progressValue() != null
                    ? healthProgress.progressValue()
                    : request == null ? null : request.progressValue();
            quest.complete(progressValue, completion.proof());
            rewardService.issueQuestCompletionRewards(quest, completion.rewardExp(), completion.rewardCurrency(), completion.rewardMemoPrefix());
            achievementBadgeService.awardForQuestCompletion(quest);
        }
        return QuestResponse.from(quest, exercisesFromContext(quest));
    }

    @Transactional
    public QuestResponse resetQuestCompletion(Long userId, Long questId) {
        UserQuest quest = userQuestRepository.findByIdAndUser_Id(questId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QUEST_NOT_FOUND", "퀘스트를 찾을 수 없습니다."));

        if (UserQuest.STATUS_COMPLETED.equals(quest.getStatus())) {
            rewardService.revokeQuestCompletionRewards(quest);
        }
        quest.resetCompletion();
        return QuestResponse.from(quest, exercisesFromContext(quest));
    }

    @Transactional
    public QuestResponse completeTodayQuestWithShopPass(Long userId, Long itemId, String itemName) {
        QuestResponse todayQuest = getTodayQuest(userId);
        if (Boolean.TRUE.equals(todayQuest.completed())) {
            throw new ApiException(HttpStatus.CONFLICT, "QUEST_ALREADY_COMPLETED", "이미 완료된 오늘 퀘스트에는 스킵권을 사용할 수 없습니다.");
        }
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("source", "shop_pass");
        proof.put("itemId", itemId);
        proof.put("itemName", itemName);
        proof.put("reason", "퀘스트 스킵권으로 완료했습니다.");
        return completeQuest(userId, todayQuest.id(), new CompleteQuestRequest(1, proof, null));
    }

    private HealthQuestProgress evaluateHealthProgress(UserQuest quest, CompleteQuestRequest request) {
        if (request == null) {
            return null;
        }
        try {
            return healthQuestProgressEvaluator.evaluate(quest, request.healthSamples());
        } catch (RuntimeException exception) {
            Map<String, Object> proof = new LinkedHashMap<>();
            proof.put("source", "health_data");
            proof.put("verified", false);
            proof.put("reason", "건강 데이터 샘플을 처리할 수 없어 수동 완료로 처리했습니다.");
            proof.put("failureType", exception.getClass().getSimpleName());
            if (exception instanceof ApiException apiException) {
                proof.put("failureCode", apiException.getCode());
            }
            return new HealthQuestProgress(null, proof, false);
        }
    }

    private QuestCompletion completionFor(UserQuest quest, CompleteQuestRequest request, HealthQuestProgress healthProgress) {
        if (healthProgress != null && healthProgress.verified()) {
            Map<String, Object> proof = new LinkedHashMap<>(healthProgress.proof());
            proof.put("completionType", COMPLETION_TYPE_VERIFIED);
            proof.put("verificationStatus", VERIFICATION_STATUS_VERIFIED);
            proof.put("battleEligible", true);
            int rewardExp = shopPassEffectService.applyExpBoost(quest.getUser().getId(), quest.getRewardExp());
            proof.put("rewardExp", rewardExp);
            if (rewardExp > quest.getRewardExp()) {
                proof.put("expBoostApplied", true);
            }
            proof.put("rewardCurrency", quest.getRewardCurrency());
            proof.put("rewardGold", quest.getRewardCurrency());
            return new QuestCompletion(proof, rewardExp, quest.getRewardCurrency(), "검증 퀘스트 완료");
        }

        Map<String, Object> proof = healthProgress == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(healthProgress.proof());
        proof.putIfAbsent("source", "manual");
        proof.put("verified", false);
        proof.put("completionType", COMPLETION_TYPE_MANUAL);
        proof.put("verificationStatus", healthProgress == null ? VERIFICATION_STATUS_NOT_PROVIDED : VERIFICATION_STATUS_INSUFFICIENT_DATA);
        proof.put("battleEligible", false);
        int rewardExp = shopPassEffectService.applyExpBoost(quest.getUser().getId(), MANUAL_COMPLETION_REWARD_EXP);
        proof.put("rewardExp", rewardExp);
        if (rewardExp > MANUAL_COMPLETION_REWARD_EXP) {
            proof.put("expBoostApplied", true);
        }
        proof.put("rewardCurrency", MANUAL_COMPLETION_REWARD_CURRENCY);
        proof.put("rewardGold", MANUAL_COMPLETION_REWARD_CURRENCY);
        if (request != null && request.proof() != null && !request.proof().isEmpty()) {
            proof.put("submittedProof", request.proof());
        }
        return new QuestCompletion(proof, rewardExp, MANUAL_COMPLETION_REWARD_CURRENCY, "수동 퀘스트 완료");
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
        context.put("exercises", recommendedExerciseContexts(List.of(
                new RecommendedExercise("걷기, 런닝머신", "유산소", 1, targetMinutes * 60)
        )));
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
        context.put("exercises", recommendedExerciseContexts(List.of(
                new RecommendedExercise("회복 요가", "요가", 1, 600)
        )));
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

    private List<Map<String, Object>> recommendedExerciseContexts(List<RecommendedExercise> recommendations) {
        Map<String, Exercise> exerciseByName = new LinkedHashMap<>();
        List<String> names = recommendations.stream()
                .map(RecommendedExercise::name)
                .toList();
        for (Exercise exercise : exerciseRepository.findByNameIn(names)) {
            exerciseByName.put(exercise.getName(), exercise);
        }

        List<Map<String, Object>> contexts = new ArrayList<>();
        for (RecommendedExercise recommendation : recommendations) {
            Exercise exercise = exerciseByName.get(recommendation.name());
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("exerciseId", exercise == null ? null : exercise.getId());
            context.put("exerciseName", exercise == null ? recommendation.name() : exercise.getName());
            context.put("category", exercise == null ? recommendation.category() : exercise.getCategory());
            context.put("seq", recommendation.seq());
            context.put("targetSets", null);
            context.put("targetReps", null);
            context.put("targetDurationSec", recommendation.targetDurationSec());
            contexts.add(context);
        }
        return contexts;
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

    private record QuestCompletion(Map<String, Object> proof,
                                   int rewardExp,
                                   int rewardCurrency,
                                   String rewardMemoPrefix) {
    }

    private record RecommendedExercise(String name,
                                       String category,
                                       int seq,
                                       int targetDurationSec) {
    }
}
