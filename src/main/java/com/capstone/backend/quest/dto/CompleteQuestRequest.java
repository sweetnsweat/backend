package com.capstone.backend.quest.dto;

import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import java.util.List;
import java.util.Map;

public record CompleteQuestRequest(
        Integer progressValue,
        Map<String, Object> proof,
        List<HealthMetricSampleRequest> healthSamples
) {
}
