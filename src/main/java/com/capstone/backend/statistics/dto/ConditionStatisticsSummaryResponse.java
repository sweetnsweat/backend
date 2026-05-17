package com.capstone.backend.statistics.dto;

import java.math.BigDecimal;

public record ConditionStatisticsSummaryResponse(
        BigDecimal averageCondition,
        Integer workoutCount,
        Integer improvementRatePercent
) {
}
