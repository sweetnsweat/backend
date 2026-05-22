package com.capstone.backend.health.dto;

import java.util.List;
import java.util.Map;

public record HealthDataSyncResponse(
        Integer acceptedSamples,
        Map<String, Integer> countByType,
        List<HealthMetricSummaryResponse> summaries
) {
}
