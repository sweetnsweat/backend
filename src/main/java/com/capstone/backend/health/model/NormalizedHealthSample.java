package com.capstone.backend.health.model;

import java.math.BigDecimal;
import java.time.Instant;

public record NormalizedHealthSample(
        HealthMetricType type,
        BigDecimal value,
        String unit,
        Instant startTime,
        Instant endTime,
        HealthDataSource source,
        String dataOrigin,
        String rawRecordType,
        String exerciseType,
        BigDecimal caloriesKcal,
        BigDecimal distanceMeters,
        Integer count,
        String customTitle
) {
}
