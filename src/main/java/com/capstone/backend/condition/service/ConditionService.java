package com.capstone.backend.condition.service;

import com.capstone.backend.condition.dto.ConditionLogResponse;
import com.capstone.backend.condition.dto.ConditionTodayRequest;
import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConditionService {

    private static final BigDecimal VERY_LOW_READINESS_THRESHOLD = BigDecimal.valueOf(40);
    private static final BigDecimal LOW_READINESS_THRESHOLD = BigDecimal.valueOf(60);
    private static final BigDecimal HIGH_READINESS_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal RECOVERY_DAY_MULTIPLIER = BigDecimal.valueOf(0.70);
    private static final BigDecimal REDUCED_LOAD_MULTIPLIER = BigDecimal.valueOf(0.85);
    private static final BigDecimal NORMAL_LOAD_MULTIPLIER = BigDecimal.valueOf(1.00);
    private static final BigDecimal CONSERVATIVE_PROGRESS_MULTIPLIER = BigDecimal.valueOf(1.10);

    private final ConditionLogRepository conditionLogRepository;
    private final UserRepository userRepository;

    public ConditionService(ConditionLogRepository conditionLogRepository, UserRepository userRepository) {
        this.conditionLogRepository = conditionLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ConditionLogResponse getTodayCondition(Long userId) {
        LocalDate today = KoreanTime.today();
        ConditionLog conditionLog = conditionLogRepository.findByUser_IdAndLogDate(userId, today)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONDITION_NOT_FOUND", "Today's condition log not found"));

        return ConditionLogResponse.from(conditionLog);
    }

    @Transactional
    public ConditionLogResponse updateTodayCondition(Long userId, ConditionTodayRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        LocalDate today = KoreanTime.today();
        BigDecimal conditionScore = calculateConditionScore(request);
        BigDecimal exerciseMultiplier = calculateExerciseMultiplier(conditionScore);

        ConditionLog conditionLog = conditionLogRepository.findByUser_IdAndLogDate(userId, today)
                .orElseGet(() -> ConditionLog.create(
                        user,
                        today,
                        request.conditionLevel(),
                        request.sleepScore(),
                        request.stressScore(),
                        fatigueScore(request),
                        request.energyLevel(),
                        conditionScore,
                        exerciseMultiplier
                ));

        conditionLog.updateScores(
                request.conditionLevel(),
                request.sleepScore(),
                request.stressScore(),
                fatigueScore(request),
                request.energyLevel(),
                conditionScore,
                exerciseMultiplier
        );

        ConditionLog savedConditionLog = conditionLogRepository.save(conditionLog);
        return ConditionLogResponse.from(savedConditionLog);
    }

    private BigDecimal calculateConditionScore(ConditionTodayRequest request) {
        // Modified Hooper-style readiness score: normalize subjective wellness inputs to 0-100 and weight them equally.
        BigDecimal total = normalizePositive(request.conditionLevel(), 1, 5)
                .add(normalizePositive(request.sleepScore(), 1, 4))
                .add(normalizeInverse(request.stressScore(), 1, 5))
                .add(normalizePositive(request.energyLevel(), 1, 5));

        BigDecimal average = total.divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP);

        return average.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositive(Integer value, int min, int max) {
        return BigDecimal.valueOf(value - min)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(max - min), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeInverse(Integer value, int min, int max) {
        return BigDecimal.valueOf(max - value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(max - min), 4, RoundingMode.HALF_UP);
    }

    private Integer fatigueScore(ConditionTodayRequest request) {
        return 6 - request.energyLevel();
    }

    private BigDecimal calculateExerciseMultiplier(BigDecimal conditionScore) {
        if (conditionScore.compareTo(VERY_LOW_READINESS_THRESHOLD) < 0) {
            return RECOVERY_DAY_MULTIPLIER.setScale(2, RoundingMode.HALF_UP);
        }
        if (conditionScore.compareTo(LOW_READINESS_THRESHOLD) < 0) {
            return REDUCED_LOAD_MULTIPLIER.setScale(2, RoundingMode.HALF_UP);
        }
        if (conditionScore.compareTo(HIGH_READINESS_THRESHOLD) < 0) {
            return NORMAL_LOAD_MULTIPLIER.setScale(2, RoundingMode.HALF_UP);
        }
        return CONSERVATIVE_PROGRESS_MULTIPLIER.setScale(2, RoundingMode.HALF_UP);
    }
}
