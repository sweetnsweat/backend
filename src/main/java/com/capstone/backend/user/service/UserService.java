package com.capstone.backend.user.service;

import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.repository.RoutineRepository;
import com.capstone.backend.user.dto.OnboardingProfileRequest;
import com.capstone.backend.user.dto.UpdateActiveRoutineRequest;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;
    private final ConditionLogRepository conditionLogRepository;

    public UserService(UserRepository userRepository,
                       RoutineRepository routineRepository,
                       ConditionLogRepository conditionLogRepository) {
        this.userRepository = userRepository;
        this.routineRepository = routineRepository;
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

        user.updateOnboardingProfile(
                request.gender(),
                request.birthDate(),
                request.heightCm(),
                request.weightKg(),
                request.experienceLevel(),
                request.preferredExerciseTypes()
        );

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
        return RoutineDetailResponse.from(routine);
    }

    @Transactional
    public RoutineDetailResponse updateActiveRoutine(Long userId, UpdateActiveRoutineRequest request) {
        User user = findUser(userId);
        Routine routine = findActiveRoutine(request.routineId());

        user.updateActiveRoutine(routine);
        return RoutineDetailResponse.from(routine);
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
        return conditionLogRepository.findByUser_IdAndLogDate(userId, LocalDate.now()).isPresent();
    }
}
