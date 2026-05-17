package com.capstone.backend.statistics.dto;

import java.math.BigDecimal;

public record ExerciseEffectResponse(
        String exerciseType,
        String displayName,
        BigDecimal averageCondition,
        Integer sampleCount
) {
}
