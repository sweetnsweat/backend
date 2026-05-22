package com.capstone.backend.health.dto;

import java.util.List;

public record HealthDataSyncRequest(
        List<HealthMetricSampleRequest> samples
) {
}
