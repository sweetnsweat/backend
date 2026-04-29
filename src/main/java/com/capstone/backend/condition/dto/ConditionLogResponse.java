package com.capstone.backend.condition.dto;

import com.capstone.backend.condition.entity.ConditionLog;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConditionLogResponse(
        Long id,
        LocalDate logDate,
        Integer conditionLevel,
        Integer sleepScore,
        Integer stressScore,
        Integer energyLevel,
        BigDecimal conditionScore,
        BigDecimal exerciseMultiplier
) {
    public static ConditionLogResponse from(ConditionLog conditionLog) {
        return new ConditionLogResponse(
                conditionLog.getId(),
                conditionLog.getLogDate(),
                conditionLevel(conditionLog),
                conditionLog.getSleepScore(),
                conditionLog.getStressScore(),
                energyLevel(conditionLog),
                conditionLog.getConditionScore(),
                conditionLog.getExerciseMultiplier()
        );
    }

    private static Integer conditionLevel(ConditionLog conditionLog) {
        if (conditionLog.getConditionLevel() != null) {
            return conditionLog.getConditionLevel();
        }
        return levelFromConditionScore(conditionLog.getConditionScore());
    }

    private static Integer energyLevel(ConditionLog conditionLog) {
        if (conditionLog.getEnergyLevel() != null) {
            return conditionLog.getEnergyLevel();
        }
        return 6 - conditionLog.getFatigueScore();
    }

    private static Integer levelFromConditionScore(BigDecimal conditionScore) {
        if (conditionScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return 5;
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return 4;
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return 3;
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return 2;
        }
        return 1;
    }
}
