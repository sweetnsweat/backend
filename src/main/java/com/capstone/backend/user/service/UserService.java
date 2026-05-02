package com.capstone.backend.user.service;

import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
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
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import com.capstone.backend.global.time.KoreanTime;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final RoutineItemRepository routineItemRepository;
    private final ConditionLogRepository conditionLogRepository;

    public UserService(UserRepository userRepository,
                       RoutineRepository routineRepository,
                       RoutineSessionRepository routineSessionRepository,
                       RoutineItemRepository routineItemRepository,
                       ConditionLogRepository conditionLogRepository) {
        this.userRepository = userRepository;
        this.routineRepository = routineRepository;
        this.routineSessionRepository = routineSessionRepository;
        this.routineItemRepository = routineItemRepository;
        this.conditionLogRepository = conditionLogRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        return UserProfileResponse.from(user, hasTodayCondition(user.getId()));
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

        return UserProfileResponse.from(user, hasTodayCondition(user.getId()));
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
