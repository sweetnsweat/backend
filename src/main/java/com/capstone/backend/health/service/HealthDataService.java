package com.capstone.backend.health.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.health.dto.HealthDataSyncRequest;
import com.capstone.backend.health.dto.HealthDataSyncResponse;
import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import com.capstone.backend.health.dto.HealthMetricSummaryResponse;
import com.capstone.backend.health.entity.HealthDailySummary;
import com.capstone.backend.health.model.HealthMetricType;
import com.capstone.backend.health.model.NormalizedHealthSample;
import com.capstone.backend.health.repository.HealthDailySummaryRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthDataService {

    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(65);
    private static final BigDecimal DEFAULT_STRIDE_LENGTH_METERS = BigDecimal.valueOf(0.70);
    private static final BigDecimal DEFAULT_WALKING_SPEED_METERS_PER_SECOND = BigDecimal.valueOf(1.33);
    private static final BigDecimal MIN_REASONABLE_SPEED_METERS_PER_SECOND = BigDecimal.valueOf(0.50);
    private static final BigDecimal MAX_REASONABLE_SPEED_METERS_PER_SECOND = BigDecimal.valueOf(7.00);
    private static final BigDecimal MAX_STEP_SAMPLE_ELAPSED_SECONDS = BigDecimal.valueOf(21600);

    private final HealthDataNormalizer normalizer;
    private final HealthDailySummaryRepository healthDailySummaryRepository;
    private final UserRepository userRepository;

    public HealthDataService(HealthDataNormalizer normalizer,
                             HealthDailySummaryRepository healthDailySummaryRepository,
                             UserRepository userRepository) {
        this.normalizer = normalizer;
        this.healthDailySummaryRepository = healthDailySummaryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public HealthDataSyncResponse summarize(Long userId, HealthDataSyncRequest request) {
        List<NormalizedHealthSample> samples = normalizeSamples(request == null ? null : request.samples());
        saveDailySummaries(userId, samples);
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

    private void saveDailySummaries(Long userId, List<NormalizedHealthSample> samples) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        BigDecimal weightKg = effectiveWeightKg(user);
        BigDecimal strideLengthMeters = effectiveStrideLengthMeters(user);
        Map<LocalDate, DailyTotals> totalsByDate = new LinkedHashMap<>();
        for (NormalizedHealthSample sample : samples) {
            LocalDate summaryDate = summaryDate(sample);
            totalsByDate.computeIfAbsent(summaryDate, ignored -> new DailyTotals(weightKg, strideLengthMeters))
                    .add(sample);
        }

        for (Map.Entry<LocalDate, DailyTotals> entry : totalsByDate.entrySet()) {
            HealthDailySummary summary = healthDailySummaryRepository
                    .findByUser_IdAndSummaryDate(userId, entry.getKey())
                    .orElseGet(() -> HealthDailySummary.create(user, entry.getKey()));
            DailyTotals totals = entry.getValue();
            summary.mergeTotals(
                    roundedInt(totals.steps),
                    roundedInt(totals.summaryDistanceMeters()),
                    roundedInt(totals.activeCaloriesKcal),
                    roundedInt(totals.exerciseMinutes),
                    totals.sampleCount
            );
            healthDailySummaryRepository.save(summary);
        }
    }

    private LocalDate summaryDate(NormalizedHealthSample sample) {
        Instant time = sample.startTime() == null ? KoreanTime.nowInstant() : sample.startTime();
        return time.atZone(KoreanTime.ZONE_ID).toLocalDate();
    }

    private int roundedInt(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal effectiveWeightKg(User user) {
        if (user.getWeightKg() == null || user.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_WEIGHT_KG;
        }
        return user.getWeightKg();
    }

    private BigDecimal effectiveStrideLengthMeters(User user) {
        if (user.getHeightCm() == null || user.getHeightCm().compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_STRIDE_LENGTH_METERS;
        }
        BigDecimal heightMeters = user.getHeightCm().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return clamp(heightMeters.multiply(BigDecimal.valueOf(0.414)), BigDecimal.valueOf(0.45), BigDecimal.valueOf(0.95));
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

    private static final class DailyTotals {
        private final BigDecimal weightKg;
        private final BigDecimal fallbackStrideLengthMeters;
        private BigDecimal steps = BigDecimal.ZERO;
        private BigDecimal explicitDistanceMeters = BigDecimal.ZERO;
        private BigDecimal estimatedStepDistanceMeters = BigDecimal.ZERO;
        private BigDecimal activeCaloriesKcal = BigDecimal.ZERO;
        private BigDecimal exerciseMinutes = BigDecimal.ZERO;
        private int sampleCount;

        private DailyTotals(BigDecimal weightKg, BigDecimal fallbackStrideLengthMeters) {
            this.weightKg = weightKg == null ? DEFAULT_WEIGHT_KG : weightKg;
            this.fallbackStrideLengthMeters = fallbackStrideLengthMeters == null
                    ? DEFAULT_STRIDE_LENGTH_METERS
                    : fallbackStrideLengthMeters;
        }

        private void add(NormalizedHealthSample sample) {
            sampleCount++;
            BigDecimal value = normalizedValue(sample);
            if (HealthMetricType.STEPS.equals(sample.type())) {
                steps = steps.add(value);
                addStepDistance(sample, value);
                activeCaloriesKcal = activeCaloriesKcal.add(estimatedStepCalories(sample, value));
                return;
            }
            if (HealthMetricType.DISTANCE.equals(sample.type())) {
                explicitDistanceMeters = explicitDistanceMeters.add(value);
                return;
            }
            if (HealthMetricType.ACTIVE_CALORIES_BURNED.equals(sample.type())
                    || HealthMetricType.TOTAL_CALORIES_BURNED.equals(sample.type())) {
                activeCaloriesKcal = activeCaloriesKcal.add(value);
                return;
            }
            if (HealthMetricType.EXERCISE_SESSION.equals(sample.type())) {
                exerciseMinutes = exerciseMinutes.add(value);
                activeCaloriesKcal = activeCaloriesKcal.add(positiveOrZero(sample.caloriesKcal()));
            }
        }

        private BigDecimal summaryDistanceMeters() {
            if (explicitDistanceMeters.compareTo(BigDecimal.ZERO) > 0) {
                return explicitDistanceMeters;
            }
            return estimatedStepDistanceMeters;
        }

        private BigDecimal normalizedValue(NormalizedHealthSample sample) {
            String unit = sample.unit() == null ? "" : sample.unit().trim().toLowerCase();
            BigDecimal value = sample.value() == null ? BigDecimal.ZERO : sample.value();
            if (HealthMetricType.DISTANCE.equals(sample.type()) && (unit.equals("km") || unit.equals("kilometer") || unit.equals("kilometers"))) {
                return value.multiply(BigDecimal.valueOf(1000));
            }
            if (HealthMetricType.EXERCISE_SESSION.equals(sample.type()) && (unit.equals("sec") || unit.equals("second") || unit.equals("seconds"))) {
                return value.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            }
            return value;
        }

        private BigDecimal estimatedStepCalories(NormalizedHealthSample sample, BigDecimal stepCount) {
            if (stepCount.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal distance = stepDistanceMeters(sample, stepCount);
            if (distance.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal speed = stepSpeedMetersPerSecond(sample, distance);
            BigDecimal durationHours = distance
                    .divide(speed, 6, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
            BigDecimal speedKmh = speed.multiply(BigDecimal.valueOf(3.6));
            BigDecimal met = walkingMet(speedKmh);
            return met.multiply(weightKg).multiply(durationHours);
        }

        private BigDecimal stepDistanceMeters(NormalizedHealthSample sample, BigDecimal stepCount) {
            BigDecimal explicitDistance = positiveOrZero(sample.distanceMeters());
            if (explicitDistance.compareTo(BigDecimal.ZERO) > 0) {
                return explicitDistance;
            }
            BigDecimal stride = sample.strideLengthMeters() == null
                    ? fallbackStrideLengthMeters
                    : clamp(sample.strideLengthMeters(), BigDecimal.valueOf(0.40), BigDecimal.valueOf(1.60));
            return stepCount.multiply(stride);
        }

        private void addStepDistance(NormalizedHealthSample sample, BigDecimal stepCount) {
            BigDecimal explicitDistance = positiveOrZero(sample.distanceMeters());
            if (explicitDistance.compareTo(BigDecimal.ZERO) > 0) {
                explicitDistanceMeters = explicitDistanceMeters.add(explicitDistance);
                return;
            }
            estimatedStepDistanceMeters = estimatedStepDistanceMeters.add(stepDistanceMeters(sample, stepCount));
        }

        private BigDecimal stepSpeedMetersPerSecond(NormalizedHealthSample sample, BigDecimal distance) {
            BigDecimal explicitSpeed = positiveOrZero(sample.speedMetersPerSecond());
            if (explicitSpeed.compareTo(BigDecimal.ZERO) > 0) {
                return clamp(explicitSpeed, MIN_REASONABLE_SPEED_METERS_PER_SECOND, MAX_REASONABLE_SPEED_METERS_PER_SECOND);
            }
            if (sample.startTime() != null && sample.endTime() != null && sample.endTime().isAfter(sample.startTime())) {
                BigDecimal elapsedSeconds = BigDecimal.valueOf(java.time.Duration.between(sample.startTime(), sample.endTime()).toMillis())
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
                if (elapsedSeconds.compareTo(BigDecimal.valueOf(60)) >= 0
                        && elapsedSeconds.compareTo(MAX_STEP_SAMPLE_ELAPSED_SECONDS) <= 0) {
                    BigDecimal speed = distance.divide(elapsedSeconds, 6, RoundingMode.HALF_UP);
                    if (speed.compareTo(MIN_REASONABLE_SPEED_METERS_PER_SECOND) >= 0
                            && speed.compareTo(MAX_REASONABLE_SPEED_METERS_PER_SECOND) <= 0) {
                        return speed;
                    }
                }
            }
            return DEFAULT_WALKING_SPEED_METERS_PER_SECOND;
        }

        private BigDecimal walkingMet(BigDecimal speedKmh) {
            if (speedKmh.compareTo(BigDecimal.valueOf(3.2)) < 0) {
                return BigDecimal.valueOf(2.8);
            }
            if (speedKmh.compareTo(BigDecimal.valueOf(4.8)) < 0) {
                return BigDecimal.valueOf(3.3);
            }
            if (speedKmh.compareTo(BigDecimal.valueOf(6.4)) < 0) {
                return BigDecimal.valueOf(4.3);
            }
            if (speedKmh.compareTo(BigDecimal.valueOf(8.0)) < 0) {
                return BigDecimal.valueOf(6.0);
            }
            return BigDecimal.valueOf(8.3);
        }

        private BigDecimal positiveOrZero(BigDecimal value) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return value;
        }
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return min;
        }
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
