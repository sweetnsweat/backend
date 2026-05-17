package com.capstone.backend.health.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HealthMetricSampleRequest(
        String type,
        BigDecimal value,
        String unit,
        Instant startTime,
        Instant endTime,
        String source,
        String dataOrigin,
        String rawRecordType
) {
}
