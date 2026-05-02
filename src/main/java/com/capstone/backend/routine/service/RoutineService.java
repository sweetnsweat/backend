package com.capstone.backend.routine.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.exercise.repository.ExerciseRepository;
import com.capstone.backend.routine.dto.CreateCustomRoutineRequest;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.dto.RoutineRecommendationResponse;
import com.capstone.backend.routine.dto.RoutineSummaryResponse;
import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.routine.entity.RoutineSession;
import com.capstone.backend.routine.repository.RoutineItemRepository;
import com.capstone.backend.routine.repository.RoutineRepository;
import com.capstone.backend.routine.repository.RoutineSessionRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final RoutineItemRepository routineItemRepository;
    private final ExerciseRepository exerciseRepository;

    public RoutineService(RoutineRepository routineRepository,
                          UserRepository userRepository,
                          RoutineSessionRepository routineSessionRepository,
                          RoutineItemRepository routineItemRepository,
                          ExerciseRepository exerciseRepository) {
        this.routineRepository = routineRepository;
        this.userRepository = userRepository;
        this.routineSessionRepository = routineSessionRepository;
        this.routineItemRepository = routineItemRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> getDefaultRoutines() {
        return routineRepository.findByDefaultRoutineTrueAndActiveTrueOrderByIdAsc().stream()
                .map(RoutineSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoutineRecommendationResponse> getRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (!user.isOnboardingCompleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ONBOARDING_REQUIRED", "온보딩 프로필을 먼저 저장해 주세요.");
        }

        return routineRepository.findByDefaultRoutineTrueAndActiveTrueOrderByIdAsc().stream()
                .map(routine -> scoreRoutine(user, routine))
                .sorted(Comparator.comparing(RoutineRecommendationResponse::score).reversed()
                        .thenComparing(response -> response.routine().id()))
                .limit(2)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse getRoutine(Long userId, Long routineId) {
        Routine routine = routineRepository.findAccessibleWithItemsByIdAndActiveTrue(routineId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "Routine not found"));
        Routine routineWithSessions = routineRepository.findWithSessionsByIdAndActiveTrue(routineId)
                .orElse(routine);

        return RoutineDetailResponse.from(routine, routineWithSessions.getSessions());
    }

    @Transactional
    public RoutineDetailResponse createCustomRoutine(Long userId, CreateCustomRoutineRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        int estimatedMinutes = request.sessions().stream()
                .map(CreateCustomRoutineRequest.SessionRequest::estimatedMinutes)
                .filter(minutes -> minutes != null)
                .mapToInt(Integer::intValue)
                .sum();
        Routine routine = routineRepository.save(Routine.createCustom(
                user,
                request.name().trim(),
                normalizeText(request.description()),
                estimatedMinutes == 0 ? null : estimatedMinutes
        ));

        Map<Long, Exercise> exerciseCache = new HashMap<>();
        int sessionSeq = 1;
        for (CreateCustomRoutineRequest.SessionRequest sessionRequest : request.sessions()) {
            RoutineSession session = routineSessionRepository.save(RoutineSession.create(
                    routine,
                    sessionRequest.dayOfWeek(),
                    sessionRequest.sessionName().trim(),
                    normalizeText(sessionRequest.sessionType()),
                    sessionSeq++,
                    sessionRequest.estimatedMinutes()
            ));
            int itemIndex = 1;
            for (CreateCustomRoutineRequest.ItemRequest itemRequest : sessionRequest.items()) {
                validateTarget(itemRequest);
                Exercise exercise = exerciseCache.computeIfAbsent(itemRequest.exerciseId(), this::findExercise);
                routineItemRepository.save(RoutineItem.create(
                        routine,
                        session,
                        exercise,
                        itemRequest.seq() == null ? itemIndex : itemRequest.seq(),
                        itemRequest.sets(),
                        itemRequest.reps(),
                        itemRequest.durationSec(),
                        itemRequest.restSec()
                ));
                itemIndex++;
            }
        }

        if (request.activate() == null || Boolean.TRUE.equals(request.activate())) {
            user.updateActiveRoutine(routine);
        }

        return routineDetail(routine.getId());
    }

    private RoutineRecommendationResponse scoreRoutine(User user, Routine routine) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (equalsValue(user.getExperienceLevel(), routine.getTargetExperienceLevel())) {
            score += 30;
            reasons.add("운동 경험 수준이 맞는 루틴입니다.");
        }
        if (containsValue(routine.getTargetCurrentExerciseStatuses(), user.getCurrentExerciseStatus())) {
            score += 15;
            reasons.add("현재 운동 상태에 맞는 시작 강도입니다.");
        }
        if (containsValue(routine.getGoalTypes(), user.getFitnessGoal())) {
            score += 25;
            reasons.add("운동 목표와 잘 맞습니다.");
        }
        if (containsValue(routine.getPlaceTypes(), user.getPreferredWorkoutPlace())) {
            score += 20;
            reasons.add("선호 운동 장소에서 수행하기 좋습니다.");
        }
        if (routine.getEstimatedMinutes() != null && user.getAvailableWorkoutMinutes() != null) {
            int minutesDifference = routine.getEstimatedMinutes() - user.getAvailableWorkoutMinutes();
            if (minutesDifference <= 0) {
                score += 10;
                reasons.add("1회 운동 가능 시간 안에 수행할 수 있습니다.");
            } else if (minutesDifference <= 10) {
                score += 5;
                reasons.add("1회 운동 가능 시간과 비교적 가깝습니다.");
            }
        }
        if (routine.getWeeklyFrequency() != null && user.getWeeklyWorkoutFrequency() != null) {
            int frequencyDifference = Math.abs(routine.getWeeklyFrequency() - user.getWeeklyWorkoutFrequency());
            if (frequencyDifference == 0) {
                score += 10;
                reasons.add("주당 운동 가능 횟수와 정확히 맞습니다.");
            } else if (frequencyDifference == 1) {
                score += 5;
                reasons.add("주당 운동 가능 횟수와 비슷합니다.");
            }
        }

        int exerciseTypeMatches = overlapCount(routine.getRecommendedExerciseTypes(), user.getPreferredExerciseTypes());
        if (exerciseTypeMatches > 0) {
            score += Math.min(exerciseTypeMatches * 5, 15);
            reasons.add("선호 운동 유형과 겹치는 운동이 포함되어 있습니다.");
        }
        if (reasons.isEmpty()) {
            reasons.add("기본 루틴 중 시작하기 쉬운 루틴입니다.");
        }

        return new RoutineRecommendationResponse(RoutineSummaryResponse.from(routine), score, reasons);
    }

    private boolean equalsValue(String left, String right) {
        return left != null && left.equals(right);
    }

    private boolean containsValue(List<String> values, String value) {
        return value != null && values != null && values.contains(value);
    }

    private int overlapCount(List<String> left, List<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String value : right) {
            if (left.contains(value)) {
                count++;
            }
        }
        return count;
    }

    private RoutineDetailResponse routineDetail(Long routineId) {
        Routine routine = routineRepository.findWithItemsByIdAndActiveTrue(routineId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "루틴을 찾을 수 없습니다."));
        Routine routineWithSessions = routineRepository.findWithSessionsByIdAndActiveTrue(routineId)
                .orElse(routine);
        return RoutineDetailResponse.from(routine, routineWithSessions.getSessions());
    }

    private Exercise findExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND", "루틴에 추가할 운동을 찾을 수 없습니다."));
    }

    private void validateTarget(CreateCustomRoutineRequest.ItemRequest itemRequest) {
        if (itemRequest.sets() == null && itemRequest.reps() == null && itemRequest.durationSec() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROUTINE_ITEM_TARGET", "운동 항목에는 세트 수, 반복 횟수, 운동 시간 중 하나 이상을 입력해 주세요.");
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
