package com.capstone.backend.condition.dto;

import com.capstone.backend.condition.entity.ConditionLog;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConditionLogResponse(
        Long id,
        LocalDate logDate,
        Integer sleepScore,
        Integer stressScore,
        Integer fatigueScore,
        BigDecimal conditionScore,
        BigDecimal exerciseMultiplier,
        String memo
) {
    public static ConditionLogResponse from(ConditionLog conditionLog) {
        return new ConditionLogResponse(
                conditionLog.getId(),
                conditionLog.getLogDate(),
                conditionLog.getSleepScore(),
                conditionLog.getStressScore(),
                conditionLog.getFatigueScore(),
                conditionLog.getConditionScore(),
                conditionLog.getExerciseMultiplier(),
                conditionLog.getMemo()
        );
    }
}
