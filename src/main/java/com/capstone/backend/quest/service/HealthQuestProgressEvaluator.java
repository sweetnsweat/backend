package com.capstone.backend.quest.service;

import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import com.capstone.backend.health.model.HealthMetricType;
import com.capstone.backend.health.model.NormalizedHealthSample;
import com.capstone.backend.health.service.HealthDataService;
import com.capstone.backend.quest.entity.UserQuest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HealthQuestProgressEvaluator {

    private final HealthDataService healthDataService;

    public HealthQuestProgressEvaluator(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    public HealthQuestProgress evaluate(UserQuest quest, List<HealthMetricSampleRequest> healthSamples) {
        if (healthSamples == null || healthSamples.isEmpty()) {
            return null;
        }

        List<NormalizedHealthSample> samples = healthDataService.normalizeSamples(healthSamples);
        Integer progressValue = progressValue(quest, samples);
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("source", "health_data");
        proof.put("summary", healthDataService.responseFrom(samples));
        return new HealthQuestProgress(progressValue, proof);
    }

    private Integer progressValue(UserQuest quest, List<NormalizedHealthSample> samples) {
        String targetMetric = quest.getTargetMetric() == null ? "" : quest.getTargetMetric().toLowerCase(Locale.ROOT);
        if ("steps".equals(targetMetric)) {
            return total(samples, HealthMetricType.STEPS).intValue();
        }
        if ("heart_rate".equals(targetMetric) || "heart_rate_max".equals(targetMetric)) {
            return max(samples, HealthMetricType.HEART_RATE).intValue();
        }
        if ("heart_rate_avg".equals(targetMetric)) {
            List<NormalizedHealthSample> heartRates = samples.stream()
                    .filter(sample -> HealthMetricType.HEART_RATE.equals(sample.type()))
                    .toList();
            if (heartRates.isEmpty()) {
                return null;
            }
            BigDecimal total = total(heartRates, HealthMetricType.HEART_RATE);
            return total.divide(BigDecimal.valueOf(heartRates.size()), 0, java.math.RoundingMode.HALF_UP).intValue();
        }
        return null;
    }

    private BigDecimal total(List<NormalizedHealthSample> samples, HealthMetricType type) {
        return samples.stream()
                .filter(sample -> type.equals(sample.type()))
                .map(NormalizedHealthSample::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal max(List<NormalizedHealthSample> samples, HealthMetricType type) {
        return samples.stream()
                .filter(sample -> type.equals(sample.type()))
                .map(NormalizedHealthSample::value)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public record HealthQuestProgress(Integer progressValue, Map<String, Object> proof) {
    }
}
