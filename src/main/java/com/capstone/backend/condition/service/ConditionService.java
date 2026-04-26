package com.capstone.backend.condition.service;

import com.capstone.backend.condition.dto.ConditionLogResponse;
import com.capstone.backend.condition.dto.ConditionTodayRequest;
import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConditionService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ConditionLogRepository conditionLogRepository;
    private final UserRepository userRepository;

    public ConditionService(ConditionLogRepository conditionLogRepository, UserRepository userRepository) {
        this.conditionLogRepository = conditionLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ConditionLogResponse getTodayCondition(Long userId) {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        ConditionLog conditionLog = conditionLogRepository.findByUser_IdAndLogDate(userId, today)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONDITION_NOT_FOUND", "Today's condition log not found"));

        return ConditionLogResponse.from(conditionLog);
    }

    @Transactional
    public ConditionLogResponse updateTodayCondition(Long userId, ConditionTodayRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        LocalDate today = LocalDate.now(SERVICE_ZONE);
        BigDecimal conditionScore = calculateConditionScore(request);
        BigDecimal exerciseMultiplier = calculateExerciseMultiplier(conditionScore);

        ConditionLog conditionLog = conditionLogRepository.findByUser_IdAndLogDate(userId, today)
                .orElseGet(() -> ConditionLog.create(
                        user,
                        today,
                        request.sleepScore(),
                        request.stressScore(),
                        request.fatigueScore(),
                        conditionScore,
                        exerciseMultiplier,
                        request.memo()
                ));

        conditionLog.updateScores(
                request.sleepScore(),
                request.stressScore(),
                request.fatigueScore(),
                conditionScore,
                exerciseMultiplier,
                request.memo()
        );

        ConditionLog savedConditionLog = conditionLogRepository.save(conditionLog);
        return ConditionLogResponse.from(savedConditionLog);
    }

    private BigDecimal calculateConditionScore(ConditionTodayRequest request) {
        int stressRecoveryScore = 6 - request.stressScore();
        int fatigueRecoveryScore = 6 - request.fatigueScore();

        BigDecimal average = BigDecimal.valueOf(request.sleepScore() + stressRecoveryScore + fatigueRecoveryScore)
                .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);

        return average.multiply(BigDecimal.valueOf(20)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateExerciseMultiplier(BigDecimal conditionScore) {
        if (conditionScore.compareTo(BigDecimal.valueOf(45)) < 0) {
            return BigDecimal.valueOf(0.70).setScale(2, RoundingMode.HALF_UP);
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(65)) < 0) {
            return BigDecimal.valueOf(0.85).setScale(2, RoundingMode.HALF_UP);
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(80)) < 0) {
            return BigDecimal.valueOf(1.00).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(1.15).setScale(2, RoundingMode.HALF_UP);
    }
}
