package com.capstone.backend.health.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.health.dto.HealthDataSyncRequest;
import com.capstone.backend.health.dto.HealthDataSyncResponse;
import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import com.capstone.backend.health.dto.HealthMetricSummaryResponse;
import com.capstone.backend.health.model.HealthMetricType;
import com.capstone.backend.health.model.NormalizedHealthSample;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HealthDataService {

    private final HealthDataNormalizer normalizer;

    public HealthDataService(HealthDataNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public HealthDataSyncResponse summarize(HealthDataSyncRequest request) {
        List<NormalizedHealthSample> samples = normalizeSamples(request == null ? null : request.samples());
        return responseFrom(samples);
    }

    public List<NormalizedHealthSample> normalizeSamples(List<HealthMetricSampleRequest> samples) {
        if (samples == null || samples.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "HEALTH_SAMPLES_REQUIRED", "건강 데이터 샘플을 1개 이상 보내 주세요.");
        }
        return samples.stream()
                .map(normalizer::normalize)
                .toList();
    }

    public HealthDataSyncResponse responseFrom(List<NormalizedHealthSample> samples) {
        Map<HealthMetricType, List<NormalizedHealthSample>> grouped = new LinkedHashMap<>();
        for (NormalizedHealthSample sample : samples) {
            grouped.computeIfAbsent(sample.type(), ignored -> new ArrayList<>()).add(sample);
        }

        Map<String, Integer> countByType = new LinkedHashMap<>();
        List<HealthMetricSummaryResponse> summaries = new ArrayList<>();
        for (Map.Entry<HealthMetricType, List<NormalizedHealthSample>> entry : grouped.entrySet()) {
            countByType.put(entry.getKey().name(), entry.getValue().size());
            summaries.add(summary(entry.getKey(), entry.getValue()));
        }
        summaries.sort(Comparator.comparing(HealthMetricSummaryResponse::type));

        return new HealthDataSyncResponse(samples.size(), countByType, summaries);
    }

    private HealthMetricSummaryResponse summary(HealthMetricType type, List<NormalizedHealthSample> samples) {
        BigDecimal total = samples.stream()
                .map(NormalizedHealthSample::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(samples.size()), 2, RoundingMode.HALF_UP);
        BigDecimal max = samples.stream()
                .map(NormalizedHealthSample::value)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        Instant firstStartTime = samples.stream()
                .map(NormalizedHealthSample::startTime)
                .filter(time -> time != null)
                .min(Instant::compareTo)
                .orElse(null);
        Instant lastEndTime = samples.stream()
                .map(sample -> sample.endTime() == null ? sample.startTime() : sample.endTime())
                .filter(time -> time != null)
                .max(Instant::compareTo)
                .orElse(null);
        List<String> dataOrigins = samples.stream()
                .map(NormalizedHealthSample::dataOrigin)
                .filter(origin -> origin != null && !origin.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        return new HealthMetricSummaryResponse(
                type.name(),
                samples.size(),
                total,
                average,
                max,
                samples.getFirst().unit(),
                firstStartTime,
                lastEndTime,
                dataOrigins
        );
    }
}
