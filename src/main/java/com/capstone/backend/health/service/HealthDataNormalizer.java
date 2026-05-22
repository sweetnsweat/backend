package com.capstone.backend.health.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import com.capstone.backend.health.model.HealthDataSource;
import com.capstone.backend.health.model.HealthMetricType;
import com.capstone.backend.health.model.NormalizedHealthSample;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HealthDataNormalizer {

    private final List<PlatformHealthDataAdapter> adapters;

    public HealthDataNormalizer(List<PlatformHealthDataAdapter> adapters) {
        this.adapters = adapters;
    }

    public NormalizedHealthSample normalize(HealthMetricSampleRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_HEALTH_SAMPLE", "건강 데이터 샘플이 비어 있습니다.");
        }
        if (request.value() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "HEALTH_VALUE_REQUIRED", "건강 데이터 값을 입력해 주세요.");
        }

        HealthDataSource source = HealthDataSource.from(request.source());
        HealthMetricType type = resolveType(request, source);
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_HEALTH_METRIC", "지원하지 않는 건강 데이터 타입입니다.");
        }

        return new NormalizedHealthSample(
                type,
                request.value(),
                normalizeUnit(request, type),
                request.startTime(),
                request.endTime() == null ? request.startTime() : request.endTime(),
                source,
                request.dataOrigin(),
                request.rawRecordType()
        );
    }

    private HealthMetricType resolveType(HealthMetricSampleRequest request, HealthDataSource source) {
        HealthMetricType explicitType = HealthMetricType.from(request.type());
        if (explicitType != null) {
            return explicitType;
        }
        return adapters.stream()
                .filter(adapter -> adapter.supports(source))
                .findFirst()
                .map(adapter -> adapter.metricType(request.rawRecordType()))
                .orElse(null);
    }

    private String normalizeUnit(HealthMetricSampleRequest request, HealthMetricType type) {
        if (request.unit() != null && !request.unit().isBlank()) {
            return request.unit().trim();
        }
        return switch (type) {
            case STEPS -> "count";
            case HEART_RATE -> "bpm";
            case DISTANCE, HEIGHT -> "m";
            case WEIGHT -> "kg";
            case TOTAL_CALORIES_BURNED, ACTIVE_CALORIES_BURNED -> "kcal";
            default -> "";
        };
    }
}
