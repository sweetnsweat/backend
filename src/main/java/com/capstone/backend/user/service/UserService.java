package com.capstone.backend.user.service;

import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.reward.repository.WalletRepository;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.dto.RoutineSummaryResponse;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.routine.entity.RoutineSession;
import com.capstone.backend.routine.repository.RoutineItemRepository;
import com.capstone.backend.routine.repository.RoutineRepository;
import com.capstone.backend.routine.repository.RoutineSessionRepository;
import com.capstone.backend.user.dto.OnboardingProfileRequest;
import com.capstone.backend.user.dto.UpdateActiveRoutineRequest;
import com.capstone.backend.user.dto.WeeklyStatsResponse;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Integer DEFAULT_CONDITION_LEVEL = 3;
    private static final Integer DEFAULT_SLEEP_SCORE = 3;
    private static final Integer DEFAULT_STRESS_SCORE = 2;
    private static final Integer DEFAULT_FATIGUE_SCORE = 3;
    private static final Integer DEFAULT_ENERGY_LEVEL = 3;
    private static final BigDecimal DEFAULT_CONDITION_SCORE = BigDecimal.valueOf(60.42);
    private static final BigDecimal DEFAULT_EXERCISE_MULTIPLIER = BigDecimal.valueOf(1.00).setScale(2);
    private static final BigDecimal FALLBACK_MALE_WEIGHT_KG = BigDecimal.valueOf(75);
    private static final BigDecimal FALLBACK_FEMALE_WEIGHT_KG = BigDecimal.valueOf(60);
    private static final BigDecimal FALLBACK_UNDISCLOSED_GENDER_WEIGHT_KG = BigDecimal.valueOf(65);
    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(70);
    private static final BigDecimal DEFAULT_ROUTINE_MET = BigDecimal.valueOf(3.5);
    private static final BigDecimal DEFAULT_OFF_DAY_MET = BigDecimal.valueOf(3.0);
    private static final BigDecimal DEFAULT_RECOVERY_MET = BigDecimal.valueOf(2.3);
    private static final int DEFAULT_ROUTINE_MINUTES = 20;

    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final RoutineItemRepository routineItemRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final UserQuestRepository userQuestRepository;
    private final WalletRepository walletRepository;

    public UserService(UserRepository userRepository,
                       RoutineRepository routineRepository,
                       RoutineSessionRepository routineSessionRepository,
                       RoutineItemRepository routineItemRepository,
                       ConditionLogRepository conditionLogRepository,
                       UserQuestRepository userQuestRepository,
                       WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.routineRepository = routineRepository;
        this.routineSessionRepository = routineSessionRepository;
        this.routineItemRepository = routineItemRepository;
        this.conditionLogRepository = conditionLogRepository;
        this.userQuestRepository = userQuestRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        return UserProfileResponse.from(user, hasTodayCondition(user.getId()), balanceCurrency(user.getId()));
    }

    @Transactional
    public UserProfileResponse updateOnboardingProfile(Long userId, OnboardingProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        boolean firstOnboardingCompletion = !user.isOnboardingCompleted();

        user.updateOnboardingProfile(
                request.gender(),
                request.birthDate(),
                request.heightCm(),
                request.weightKg(),
                request.experienceLevel(),
                request.currentExerciseStatus(),
                request.fitnessGoal(),
                request.preferredWorkoutPlace(),
                request.weeklyWorkoutFrequency(),
                request.availableWorkoutMinutes(),
                request.preferredExerciseTypes()
        );
        if (firstOnboardingCompletion) {
            createDefaultTodayConditionIfAbsent(user);
        }

        return UserProfileResponse.from(user, hasTodayCondition(user.getId()), balanceCurrency(user.getId()));
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse getActiveRoutine(Long userId) {
        User user = findUser(userId);
        Routine activeRoutine = user.getActiveRoutine();
        if (activeRoutine == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACTIVE_ROUTINE_NOT_SET", "Active routine is not set");
        }

        Routine routine = findActiveRoutine(activeRoutine.getId());
        Routine routineWithSessions = routineRepository.findWithSessionsByIdAndActiveTrue(routine.getId())
                .orElse(routine);
        return RoutineDetailResponse.from(routine, routineWithSessions.getSessions());
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> getMyRoutines(Long userId) {
        User user = findUser(userId);
        Long activeRoutineId = user.getActiveRoutine() == null ? null : user.getActiveRoutine().getId();

        return routineRepository.findByUser_IdAndActiveTrueOrderByIdDesc(userId).stream()
                .map(routine -> RoutineSummaryResponse.from(routine, routine.getId().equals(activeRoutineId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        User user = findUser(userId);
        LocalDate today = KoreanTime.today();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<UserQuest> completedQuests = userQuestRepository.findCompletedStatsByUserIdAndQuestDateBetween(
                userId,
                weekStart,
                weekEnd
        );

        int completedWorkoutCount = completedQuests.size();
        int maxStreakDays = maxStreakDays(completedQuests);
        BigDecimal weightKg = resolveEffectiveWeightKg(user);
        int estimatedCalories = completedQuests.stream()
                .map(quest -> estimatedCalories(quest, weightKg))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int earnedExp = completedQuests.stream()
                .map(UserQuest::getRewardExp)
                .filter(exp -> exp != null)
                .mapToInt(Integer::intValue)
                .sum();

        return new WeeklyStatsResponse(
                weekStart,
                weekEnd,
                completedWorkoutCount,
                maxStreakDays,
                estimatedCalories,
                earnedExp
        );
    }

    @Transactional
    public RoutineDetailResponse updateActiveRoutine(Long userId, UpdateActiveRoutineRequest request) {
        return activateRoutine(userId, request.routineId());
    }

    @Transactional
    public RoutineDetailResponse activateRoutine(Long userId, Long routineId) {
        User user = findUser(userId);
        Routine selectedRoutine = routineRepository.findAccessibleWithItemsByIdAndActiveTrue(routineId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "활성화할 수 있는 루틴을 찾을 수 없습니다."));

        Routine activeRoutine = Boolean.TRUE.equals(selectedRoutine.getDefaultRoutine())
                ? copyDefaultRoutineForUser(user, selectedRoutine)
                : selectedRoutine;

        user.updateActiveRoutine(activeRoutine);
        return routineDetail(activeRoutine.getId());
    }

    private Routine copyDefaultRoutineForUser(User user, Routine sourceRoutine) {
        return routineRepository.findByUser_IdAndSourceRoutine_IdAndActiveTrue(user.getId(), sourceRoutine.getId())
                .orElseGet(() -> createRoutineCopy(user, sourceRoutine));
    }

    private Routine createRoutineCopy(User user, Routine sourceRoutine) {
        List<RoutineItem> sourceItems = routineItemRepository.findWithExerciseByRoutineIdOrderBySeqAsc(sourceRoutine.getId());
        List<RoutineSession> sourceSessions = routineSessionRepository.findByRoutine_IdOrderBySeqAsc(sourceRoutine.getId());
        Routine routineCopy = routineRepository.save(Routine.copyFromDefault(sourceRoutine, user));

        Map<Long, RoutineSession> copiedSessionsBySourceId = new HashMap<>();
        for (RoutineSession sourceSession : sourceSessions) {
            RoutineSession copiedSession = routineSessionRepository.save(RoutineSession.copyForRoutine(routineCopy, sourceSession));
            copiedSessionsBySourceId.put(sourceSession.getId(), copiedSession);
        }
        for (RoutineItem sourceItem : sourceItems) {
            RoutineSession copiedSession = sourceItem.getRoutineSession() == null
                    ? null
                    : copiedSessionsBySourceId.get(sourceItem.getRoutineSession().getId());
            routineItemRepository.save(RoutineItem.copyForRoutine(routineCopy, copiedSession, sourceItem));
        }
        return routineCopy;
    }

    private RoutineDetailResponse routineDetail(Long routineId) {
        Routine routine = findActiveRoutine(routineId);
        Routine routineWithSessions = routineRepository.findWithSessionsByIdAndActiveTrue(routine.getId())
                .orElse(routine);
        return RoutineDetailResponse.from(routine, routineWithSessions.getSessions());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private Routine findActiveRoutine(Long routineId) {
        return routineRepository.findWithItemsByIdAndActiveTrue(routineId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "Routine not found"));
    }

    private boolean hasTodayCondition(Long userId) {
        return conditionLogRepository.findByUser_IdAndLogDate(userId, KoreanTime.today()).isPresent();
    }

    private int balanceCurrency(Long userId) {
        return walletRepository.findById(userId)
                .map(wallet -> wallet.getBalanceCurrency())
                .orElse(0);
    }

    private int maxStreakDays(List<UserQuest> quests) {
        Set<LocalDate> completedDates = new HashSet<>();
        for (UserQuest quest : quests) {
            completedDates.add(quest.getQuestDate());
        }
        int current = 0;
        int max = 0;
        LocalDate weekStart = KoreanTime.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            if (completedDates.contains(date)) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    private BigDecimal estimatedCalories(UserQuest quest, BigDecimal weightKg) {
        if (UserQuest.TYPE_ROUTINE.equals(quest.getQuestType())) {
            return estimatedRoutineCalories(quest, weightKg);
        }
        BigDecimal minutes = BigDecimal.valueOf(quest.getTargetValue() == null ? 0 : quest.getTargetValue());
        BigDecimal met = UserQuest.TYPE_RECOVERY.equals(quest.getQuestType()) ? DEFAULT_RECOVERY_MET : DEFAULT_OFF_DAY_MET;
        return met.multiply(weightKg).multiply(minutes.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal estimatedRoutineCalories(UserQuest quest, BigDecimal weightKg) {
        RoutineSession session = quest.getSourceSession();
        if (session == null || session.getItems().isEmpty()) {
            int minutes = quest.getRoutine() == null || quest.getRoutine().getEstimatedMinutes() == null
                    ? DEFAULT_ROUTINE_MINUTES
                    : quest.getRoutine().getEstimatedMinutes();
            return DEFAULT_ROUTINE_MET.multiply(weightKg).multiply(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP));
        }

        List<RoutineItem> items = session.getItems();
        BigDecimal fallbackItemMinutes = BigDecimal.valueOf(
                session.getEstimatedMinutes() == null ? DEFAULT_ROUTINE_MINUTES : session.getEstimatedMinutes()
        ).divide(BigDecimal.valueOf(items.size()), 4, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO;
        for (RoutineItem item : items) {
            BigDecimal met = item.getExercise().getMet() == null ? DEFAULT_ROUTINE_MET : item.getExercise().getMet();
            BigDecimal minutes = item.getDurationSec() == null
                    ? fallbackItemMinutes
                    : BigDecimal.valueOf(item.getDurationSec()).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            total = total.add(met.multiply(weightKg).multiply(minutes.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP)));
        }
        return total;
    }

    private BigDecimal resolveEffectiveWeightKg(User user) {
        if (user.getWeightKg() != null) {
            return user.getWeightKg();
        }
        return switch (user.getGender() == null ? "" : user.getGender()) {
            case "male" -> FALLBACK_MALE_WEIGHT_KG;
            case "female" -> FALLBACK_FEMALE_WEIGHT_KG;
            case "prefer_not_to_say" -> FALLBACK_UNDISCLOSED_GENDER_WEIGHT_KG;
            default -> DEFAULT_WEIGHT_KG;
        };
    }

    private void createDefaultTodayConditionIfAbsent(User user) {
        conditionLogRepository.findByUser_IdAndLogDate(user.getId(), KoreanTime.today())
                .orElseGet(() -> conditionLogRepository.save(ConditionLog.create(
                        user,
                        KoreanTime.today(),
                        DEFAULT_CONDITION_LEVEL,
                        DEFAULT_SLEEP_SCORE,
                        DEFAULT_STRESS_SCORE,
                        DEFAULT_FATIGUE_SCORE,
                        DEFAULT_ENERGY_LEVEL,
                        DEFAULT_CONDITION_SCORE,
                        DEFAULT_EXERCISE_MULTIPLIER
                )));
    }
}
