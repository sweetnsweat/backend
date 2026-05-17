package com.capstone.backend.health.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record HealthMetricSummaryResponse(
        String type,
        Integer count,
        BigDecimal total,
        BigDecimal average,
        BigDecimal max,
        String unit,
        Instant firstStartTime,
        Instant lastEndTime,
        List<String> dataOrigins
) {
}
