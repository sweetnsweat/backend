package com.capstone.backend.quest.service;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import com.capstone.backend.health.model.HealthMetricType;
import com.capstone.backend.health.model.NormalizedHealthSample;
import com.capstone.backend.health.service.HealthDataService;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.routine.entity.RoutineSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HealthQuestProgressEvaluator {

    private static final Duration START_GRACE = Duration.ofMinutes(5);
    private static final Duration END_GRACE = Duration.ofMinutes(2);
    private static final BigDecimal COMPLETION_THRESHOLD = BigDecimal.valueOf(0.60);
    private static final BigDecimal CALORIE_THRESHOLD = BigDecimal.valueOf(0.50);
    private static final BigDecimal PARTIAL_SESSION_THRESHOLD = BigDecimal.valueOf(0.40);
    private static final BigDecimal MODERATE_HR_RATIO = BigDecimal.valueOf(0.50);
    private static final BigDecimal VIGOROUS_HR_RATIO = BigDecimal.valueOf(0.70);

    private final HealthDataService healthDataService;

    public HealthQuestProgressEvaluator(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    public HealthQuestProgress evaluate(UserQuest quest, List<HealthMetricSampleRequest> healthSamples) {
        if (healthSamples == null || healthSamples.isEmpty()) {
            return null;
        }

        List<NormalizedHealthSample> samples = healthDataService.normalizeSamples(healthSamples);
        Instant requestedAt = KoreanTime.nowInstant();
        VerificationWindow window = verificationWindow(quest, requestedAt);
        List<WindowedSample> validSamples = windowedSamples(samples, window);
        QuestRule rule = QuestRule.from(quest);
        Metrics metrics = Metrics.from(validSamples);
        VerificationResult result = verify(quest, rule, metrics);

        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("source", "health_data");
        proof.put("verified", result.verified());
        proof.put("confidence", result.confidence());
        proof.put("rule", result.ruleCode());
        proof.put("matchedMetrics", result.matchedMetrics());
        proof.put("reason", result.reason());
        proof.put("verificationWindow", window.toMap());
        proof.put("excludedSampleCount", samples.size() - validSamples.size());
        proof.put("metrics", metrics.toMap());
        proof.put("summary", healthDataService.responseFrom(samples));

        return new HealthQuestProgress(progressValue(quest, metrics, result), proof, result.verified());
    }

    private VerificationWindow verificationWindow(UserQuest quest, Instant requestedAt) {
        Instant createdAt = quest.getCreatedAt() == null ? requestedAt : quest.getCreatedAt();
        return new VerificationWindow(
                createdAt.minus(START_GRACE),
                requestedAt.plus(END_GRACE),
                requestedAt
        );
    }

    private List<WindowedSample> windowedSamples(List<NormalizedHealthSample> samples, VerificationWindow window) {
        List<WindowedSample> validSamples = new ArrayList<>();
        for (NormalizedHealthSample sample : samples) {
            Instant sampleStart = sample.startTime();
            Instant sampleEnd = sample.endTime() == null ? sample.startTime() : sample.endTime();
            if (sampleStart == null || sampleEnd == null) {
                continue;
            }
            if (sampleEnd.isBefore(sampleStart)) {
                Instant swap = sampleStart;
                sampleStart = sampleEnd;
                sampleEnd = swap;
            }
            if (sampleEnd.isBefore(window.startTime()) || sampleStart.isAfter(window.endTime())) {
                continue;
            }

            Instant clippedStart = sampleStart.isBefore(window.startTime()) ? window.startTime() : sampleStart;
            Instant clippedEnd = sampleEnd.isAfter(window.endTime()) ? window.endTime() : sampleEnd;
            BigDecimal adjustedValue = adjustedValue(sample, sampleStart, sampleEnd, clippedStart, clippedEnd);
            validSamples.add(new WindowedSample(sample, adjustedValue, clippedStart, clippedEnd));
        }
        return validSamples;
    }

    private BigDecimal adjustedValue(NormalizedHealthSample sample,
                                     Instant sampleStart,
                                     Instant sampleEnd,
                                     Instant clippedStart,
                                     Instant clippedEnd) {
        if (!isAdditive(sample.type()) || !sampleEnd.isAfter(sampleStart)) {
            return sample.value();
        }
        BigDecimal totalMillis = BigDecimal.valueOf(Duration.between(sampleStart, sampleEnd).toMillis());
        BigDecimal clippedMillis = BigDecimal.valueOf(Duration.between(clippedStart, clippedEnd).toMillis());
        if (totalMillis.compareTo(BigDecimal.ZERO) <= 0 || clippedMillis.compareTo(BigDecimal.ZERO) <= 0) {
            return sample.value();
        }
        return sample.value().multiply(clippedMillis).divide(totalMillis, 4, RoundingMode.HALF_UP);
    }

    private boolean isAdditive(HealthMetricType type) {
        return switch (type) {
            case STEPS, DISTANCE, EXERCISE_SESSION, TOTAL_CALORIES_BURNED, ACTIVE_CALORIES_BURNED -> true;
            default -> false;
        };
    }

    private VerificationResult verify(UserQuest quest, QuestRule rule, Metrics metrics) {
        List<String> matchedMetrics = new ArrayList<>();
        BigDecimal primaryRatio = primaryRatio(rule, metrics);
        BigDecimal durationRatio = ratio(metrics.exerciseMinutes(), BigDecimal.valueOf(rule.targetMinutes()));
        BigDecimal calorieRatio = ratio(metrics.activeCaloriesKcal(), BigDecimal.valueOf(rule.targetCaloriesKcal()));
        BigDecimal countRatio = ratio(BigDecimal.valueOf(metrics.exerciseCount()), BigDecimal.valueOf(rule.targetCount()));
        HeartRateEvidence heartRateEvidence = heartRateEvidence(quest, metrics);
        boolean sessionMatched = metrics.sessionMatched(rule.keywords());

        if (primaryRatio.compareTo(COMPLETION_THRESHOLD) >= 0) {
            matchedMetrics.add(rule.primaryMetricName());
        }
        if (durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0) {
            matchedMetrics.add("exercise_duration");
        }
        if (calorieRatio.compareTo(CALORIE_THRESHOLD) >= 0) {
            matchedMetrics.add("active_calories");
        }
        if (sessionMatched) {
            matchedMetrics.add("exercise_session_type");
        }
        if (countRatio.compareTo(COMPLETION_THRESHOLD) >= 0) {
            matchedMetrics.add("exercise_count");
        }
        if (heartRateEvidence.moderate()) {
            matchedMetrics.add("heart_rate");
        }

        boolean verified = switch (rule.category()) {
            case WALKING -> primaryRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0 && metrics.steps() >= 300
                    || sessionMatched && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0;
            case RUNNING, CYCLING, SWIMMING, CARDIO -> primaryRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || calorieRatio.compareTo(CALORIE_THRESHOLD) >= 0
                    || sessionMatched && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0
                    || heartRateEvidence.moderate() && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0;
            case STRENGTH -> durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || calorieRatio.compareTo(CALORIE_THRESHOLD) >= 0
                    || countRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || sessionMatched && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0
                    || heartRateEvidence.moderate() && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0;
            case YOGA, STRETCHING, RECOVERY -> durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || sessionMatched && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0
                    || heartRateEvidence.moderate() && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0;
            case GENERAL -> primaryRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0
                    || calorieRatio.compareTo(CALORIE_THRESHOLD) >= 0
                    || heartRateEvidence.moderate() && durationRatio.compareTo(PARTIAL_SESSION_THRESHOLD) >= 0;
        };

        BigDecimal confidence = confidence(primaryRatio, durationRatio, calorieRatio, countRatio, heartRateEvidence, sessionMatched, rule);
        String reason = reason(rule, verified, metrics, primaryRatio, durationRatio, calorieRatio, heartRateEvidence, sessionMatched);
        return new VerificationResult(verified, confidence, rule.code(), matchedMetrics, reason);
    }

    private BigDecimal primaryRatio(QuestRule rule, Metrics metrics) {
        return switch (rule.primaryMetric()) {
            case STEPS -> ratio(BigDecimal.valueOf(metrics.steps()), BigDecimal.valueOf(rule.targetSteps()));
            case DISTANCE -> ratio(metrics.distanceMeters(), BigDecimal.valueOf(rule.targetDistanceMeters()));
            case TOTAL_CALORIES_BURNED, ACTIVE_CALORIES_BURNED -> ratio(metrics.activeCaloriesKcal(), BigDecimal.valueOf(rule.targetCaloriesKcal()));
            case EXERCISE_SESSION -> ratio(metrics.exerciseMinutes(), BigDecimal.valueOf(rule.targetMinutes()));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal confidence(BigDecimal primaryRatio,
                                  BigDecimal durationRatio,
                                  BigDecimal calorieRatio,
                                  BigDecimal countRatio,
                                  HeartRateEvidence heartRateEvidence,
                                  boolean sessionMatched,
                                  QuestRule rule) {
        BigDecimal score = capped(primaryRatio).multiply(BigDecimal.valueOf(0.45))
                .add(capped(durationRatio).multiply(BigDecimal.valueOf(0.30)))
                .add(capped(calorieRatio).multiply(BigDecimal.valueOf(0.10)))
                .add(capped(countRatio).multiply(BigDecimal.valueOf(0.10)));
        if (sessionMatched) {
            score = score.add(BigDecimal.valueOf(0.10));
        }
        if (heartRateEvidence.vigorous() && rule.heartRateHelpful()) {
            score = score.add(BigDecimal.valueOf(0.10));
        } else if (heartRateEvidence.moderate() && rule.heartRateHelpful()) {
            score = score.add(BigDecimal.valueOf(0.06));
        }
        return score.min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal capped(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String reason(QuestRule rule,
                          boolean verified,
                          Metrics metrics,
                          BigDecimal primaryRatio,
                          BigDecimal durationRatio,
                          BigDecimal calorieRatio,
                          HeartRateEvidence heartRateEvidence,
                          boolean sessionMatched) {
        if (!verified) {
            return rule.displayName() + " 퀘스트 기준에 필요한 건강 데이터가 부족합니다. "
                    + "운동 시간 " + format(metrics.exerciseMinutes()) + "/" + rule.targetMinutes() + "분, "
                    + rule.primaryMetricLabel() + " 달성률 " + percent(primaryRatio) + "입니다.";
        }

        List<String> parts = new ArrayList<>();
        if (primaryRatio.compareTo(COMPLETION_THRESHOLD) >= 0) {
            parts.add(rule.primaryMetricLabel() + " " + percent(primaryRatio));
        }
        if (durationRatio.compareTo(COMPLETION_THRESHOLD) >= 0) {
            parts.add("운동 시간 " + percent(durationRatio));
        }
        if (calorieRatio.compareTo(CALORIE_THRESHOLD) >= 0) {
            parts.add("활동 칼로리 " + percent(calorieRatio));
        }
        if (metrics.exerciseCount() > 0) {
            parts.add("운동 반복 수 " + metrics.exerciseCount() + "회");
        }
        if (sessionMatched) {
            parts.add("운동 세션 유형 일치");
        }
        if (heartRateEvidence.moderate() && rule.heartRateHelpful()) {
            parts.add("심박 상승 확인");
        }
        return rule.displayName() + " 퀘스트 수행 증거가 확인되었습니다: " + String.join(", ", parts);
    }

    private HeartRateEvidence heartRateEvidence(UserQuest quest, Metrics metrics) {
        BigDecimal averageHeartRate = metrics.averageHeartRate();
        BigDecimal maxHeartRate = metrics.maxHeartRate();
        if (averageHeartRate == null && maxHeartRate == null) {
            return new HeartRateEvidence(false, false);
        }
        Integer age = age(quest);
        int estimatedMaxHeartRate = age == null ? 190 : Math.max(120, 220 - age);
        BigDecimal moderateThreshold = BigDecimal.valueOf(estimatedMaxHeartRate).multiply(MODERATE_HR_RATIO);
        BigDecimal vigorousThreshold = BigDecimal.valueOf(estimatedMaxHeartRate).multiply(VIGOROUS_HR_RATIO);

        boolean moderate = (averageHeartRate != null && averageHeartRate.compareTo(moderateThreshold) >= 0)
                || (maxHeartRate != null && maxHeartRate.compareTo(moderateThreshold) >= 0);
        boolean vigorous = (averageHeartRate != null && averageHeartRate.compareTo(vigorousThreshold) >= 0)
                || (maxHeartRate != null && maxHeartRate.compareTo(vigorousThreshold) >= 0);
        return new HeartRateEvidence(moderate, vigorous);
    }

    private Integer age(UserQuest quest) {
        if (quest.getUser() == null || quest.getUser().getBirthDate() == null) {
            return null;
        }
        LocalDate today = KoreanTime.today();
        return Period.between(quest.getUser().getBirthDate(), today).getYears();
    }

    private Integer progressValue(UserQuest quest, Metrics metrics, VerificationResult result) {
        if (!result.verified()) {
            return null;
        }
        String targetMetric = quest.getTargetMetric() == null ? "" : quest.getTargetMetric().toLowerCase(Locale.ROOT);
        return switch (targetMetric) {
            case "steps" -> metrics.steps();
            case "calories" -> metrics.activeCaloriesKcal().setScale(0, RoundingMode.HALF_UP).intValue();
            case "minutes" -> metrics.exerciseMinutes().setScale(0, RoundingMode.HALF_UP).intValue();
            default -> quest.getTargetValue();
        };
    }

    private BigDecimal ratio(BigDecimal value, BigDecimal target) {
        if (value == null || target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(target, 4, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        return value == null ? "0" : value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String percent(BigDecimal ratio) {
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%";
    }

    public record HealthQuestProgress(Integer progressValue, Map<String, Object> proof, boolean verified) {
    }

    private record VerificationWindow(Instant startTime, Instant endTime, Instant requestedAt) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("requestedAt", requestedAt);
            map.put("startGraceMinutes", START_GRACE.toMinutes());
            map.put("endGraceMinutes", END_GRACE.toMinutes());
            return map;
        }
    }

    private record WindowedSample(NormalizedHealthSample sample,
                                  BigDecimal adjustedValue,
                                  Instant clippedStartTime,
                                  Instant clippedEndTime) {
    }

    private record VerificationResult(boolean verified,
                                      BigDecimal confidence,
                                      String ruleCode,
                                      List<String> matchedMetrics,
                                      String reason) {
    }

    private record HeartRateEvidence(boolean moderate, boolean vigorous) {
    }

    private record Metrics(List<WindowedSample> samples,
                           BigDecimal exerciseMinutes,
                           Integer steps,
                           BigDecimal distanceMeters,
                           BigDecimal activeCaloriesKcal,
                           Integer exerciseCount,
                           BigDecimal averageHeartRate,
                           BigDecimal maxHeartRate,
                           List<String> sessionHints) {

        static Metrics from(List<WindowedSample> samples) {
            BigDecimal exerciseMinutes = exerciseMinutes(samples);
            Integer steps = total(samples, HealthMetricType.STEPS).setScale(0, RoundingMode.HALF_UP).intValue();
            BigDecimal distanceMeters = total(samples, HealthMetricType.DISTANCE).add(sessionDistanceMeters(samples));
            BigDecimal activeCalories = total(samples, HealthMetricType.ACTIVE_CALORIES_BURNED)
                    .add(total(samples, HealthMetricType.TOTAL_CALORIES_BURNED))
                    .add(sessionCalories(samples));
            int exerciseCount = samples.stream()
                    .filter(sample -> HealthMetricType.EXERCISE_SESSION.equals(sample.sample().type()))
                    .map(WindowedSample::sample)
                    .map(NormalizedHealthSample::count)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            List<BigDecimal> heartRates = samples.stream()
                    .filter(sample -> HealthMetricType.HEART_RATE.equals(sample.sample().type()))
                    .map(WindowedSample::adjustedValue)
                    .toList();
            BigDecimal averageHeartRate = heartRates.isEmpty()
                    ? null
                    : heartRates.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(heartRates.size()), 2, RoundingMode.HALF_UP);
            BigDecimal maxHeartRate = heartRates.stream().max(BigDecimal::compareTo).orElse(null);
            List<String> sessionHints = samples.stream()
                    .filter(sample -> HealthMetricType.EXERCISE_SESSION.equals(sample.sample().type()))
                    .map(sample -> String.join(" ",
                            nonBlank(sample.sample().rawRecordType()),
                            nonBlank(sample.sample().exerciseType()),
                            nonBlank(sample.sample().customTitle()),
                            nonBlank(sample.sample().dataOrigin())))
                    .filter(text -> text != null && !text.isBlank())
                    .map(text -> text.toLowerCase(Locale.ROOT))
                    .toList();
            return new Metrics(samples, exerciseMinutes, steps, distanceMeters, activeCalories, exerciseCount, averageHeartRate, maxHeartRate, sessionHints);
        }

        private static String nonBlank(String value) {
            return value == null ? "" : value;
        }

        private static BigDecimal exerciseMinutes(List<WindowedSample> samples) {
            BigDecimal sessionMinutes = samples.stream()
                    .filter(sample -> HealthMetricType.EXERCISE_SESSION.equals(sample.sample().type()))
                    .map(Metrics::durationMinutes)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sessionMinutes.compareTo(BigDecimal.ZERO) > 0) {
                return sessionMinutes;
            }
            Instant first = samples.stream().map(WindowedSample::clippedStartTime).min(Instant::compareTo).orElse(null);
            Instant last = samples.stream().map(WindowedSample::clippedEndTime).max(Instant::compareTo).orElse(null);
            if (first == null || last == null || !last.isAfter(first)) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(Duration.between(first, last).toSeconds())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        private static BigDecimal durationMinutes(WindowedSample sample) {
            String unit = sample.sample().unit() == null ? "" : sample.sample().unit().toLowerCase(Locale.ROOT);
            if (unit.equals("minute") || unit.equals("minutes") || unit.equals("min")) {
                return sample.adjustedValue();
            }
            if (unit.equals("second") || unit.equals("seconds") || unit.equals("sec") || unit.equals("s")) {
                return sample.adjustedValue().divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            }
            if (unit.equals("millisecond") || unit.equals("milliseconds") || unit.equals("ms")) {
                return sample.adjustedValue().divide(BigDecimal.valueOf(60000), 2, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(Duration.between(sample.clippedStartTime(), sample.clippedEndTime()).toSeconds())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        private static BigDecimal total(List<WindowedSample> samples, HealthMetricType type) {
            return samples.stream()
                    .filter(sample -> type.equals(sample.sample().type()))
                    .map(sample -> normalizeValue(sample.adjustedValue(), sample.sample().unit(), type))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private static BigDecimal sessionCalories(List<WindowedSample> samples) {
            return samples.stream()
                    .filter(sample -> HealthMetricType.EXERCISE_SESSION.equals(sample.sample().type()))
                    .map(WindowedSample::sample)
                    .map(NormalizedHealthSample::caloriesKcal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private static BigDecimal sessionDistanceMeters(List<WindowedSample> samples) {
            return samples.stream()
                    .filter(sample -> HealthMetricType.EXERCISE_SESSION.equals(sample.sample().type()))
                    .map(WindowedSample::sample)
                    .map(NormalizedHealthSample::distanceMeters)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private static BigDecimal normalizeValue(BigDecimal value, String unit, HealthMetricType type) {
            if (value == null) {
                return BigDecimal.ZERO;
            }
            String normalizedUnit = unit == null ? "" : unit.toLowerCase(Locale.ROOT);
            if (HealthMetricType.DISTANCE.equals(type) && (normalizedUnit.equals("km") || normalizedUnit.equals("kilometer") || normalizedUnit.equals("kilometers"))) {
                return value.multiply(BigDecimal.valueOf(1000));
            }
            return value;
        }

        boolean sessionMatched(Set<String> keywords) {
            if (keywords.isEmpty() || sessionHints.isEmpty()) {
                return false;
            }
            return sessionHints.stream().anyMatch(hint -> keywords.stream().anyMatch(hint::contains));
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("validSampleCount", samples.size());
            map.put("exerciseMinutes", exerciseMinutes);
            map.put("steps", steps);
            map.put("distanceMeters", distanceMeters);
            map.put("activeCaloriesKcal", activeCaloriesKcal);
            map.put("exerciseCount", exerciseCount);
            map.put("averageHeartRate", averageHeartRate);
            map.put("maxHeartRate", maxHeartRate);
            map.put("sessionHints", sessionHints);
            return map;
        }
    }

    private record QuestRule(ExerciseCategory category,
                             HealthMetricType primaryMetric,
                             int targetMinutes,
                             int targetSteps,
                             int targetDistanceMeters,
                             int targetCaloriesKcal,
                             int targetCount,
                             String code,
                             String displayName,
                             Set<String> keywords) {

        static QuestRule from(UserQuest quest) {
            ExerciseCategory category = ExerciseCategory.from(quest);
            int targetMinutes = targetMinutes(quest, category);
            int targetSteps = Math.max(800, targetMinutes * switch (category) {
                case RUNNING -> 150;
                case WALKING -> 100;
                default -> 60;
            });
            int targetDistanceMeters = Math.max(500, targetMinutes * switch (category) {
                case RUNNING -> 120;
                case CYCLING -> 300;
                case SWIMMING -> 35;
                case WALKING -> 70;
                default -> 50;
            });
            int targetCalories = Math.max(40, targetMinutes * switch (category) {
                case RUNNING, CYCLING, SWIMMING, CARDIO -> 7;
                case STRENGTH -> 5;
                case WALKING -> 4;
                case YOGA, STRETCHING, RECOVERY -> 3;
                case GENERAL -> 4;
            });
            int targetCount = targetCount(quest);
            HealthMetricType primaryMetric = switch (category) {
                case WALKING -> HealthMetricType.STEPS;
                case RUNNING, CYCLING, SWIMMING, CARDIO -> HealthMetricType.DISTANCE;
                case STRENGTH, YOGA, STRETCHING, RECOVERY, GENERAL -> HealthMetricType.EXERCISE_SESSION;
            };
            return new QuestRule(
                    category,
                    primaryMetric,
                    targetMinutes,
                    targetSteps,
                    targetDistanceMeters,
                    targetCalories,
                    targetCount,
                    category.name().toLowerCase(Locale.ROOT) + "_health_proof",
                    category.displayName(),
                    category.keywords()
            );
        }

        private static int targetMinutes(UserQuest quest, ExerciseCategory category) {
            String targetMetric = quest.getTargetMetric() == null ? "" : quest.getTargetMetric().toLowerCase(Locale.ROOT);
            if ("minutes".equals(targetMetric) && quest.getTargetValue() != null) {
                return Math.max(1, quest.getTargetValue());
            }
            RoutineSession sourceSession = quest.getSourceSession();
            if (sourceSession != null && sourceSession.getEstimatedMinutes() != null) {
                return Math.max(1, sourceSession.getEstimatedMinutes());
            }
            Integer itemMinutes = itemTargetMinutes(sourceSession);
            if (itemMinutes != null) {
                return itemMinutes;
            }
            return switch (category) {
                case RECOVERY, STRETCHING -> 10;
                default -> 20;
            };
        }

        private static Integer itemTargetMinutes(RoutineSession sourceSession) {
            if (sourceSession == null) {
                return null;
            }
            int seconds = sourceSession.getItems().stream()
                    .map(RoutineItem::getDurationSec)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            return seconds <= 0 ? null : Math.max(1, (int) Math.ceil(seconds / 60.0));
        }

        private static int targetCount(UserQuest quest) {
            Object rawExercises = quest.getQuestContextJson().get("exercises");
            if (!(rawExercises instanceof List<?> exercises)) {
                return 1;
            }
            int total = 0;
            for (Object rawExercise : exercises) {
                if (rawExercise instanceof Map<?, ?> exercise) {
                    Integer sets = intValue(exercise.get("targetSets"));
                    Integer reps = intValue(exercise.get("targetReps"));
                    if (sets != null && reps != null) {
                        total += sets * reps;
                    } else if (reps != null) {
                        total += reps;
                    }
                }
            }
            return Math.max(1, total);
        }

        private static Integer intValue(Object value) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String string && !string.isBlank()) {
                return Integer.valueOf(string);
            }
            return null;
        }

        String primaryMetricName() {
            return primaryMetric.name().toLowerCase(Locale.ROOT);
        }

        String primaryMetricLabel() {
            return switch (primaryMetric) {
                case STEPS -> "걸음 수";
                case DISTANCE -> "거리";
                case ACTIVE_CALORIES_BURNED, TOTAL_CALORIES_BURNED -> "활동 칼로리";
                default -> "운동 시간";
            };
        }

        boolean heartRateHelpful() {
            return switch (category) {
                case WALKING, RUNNING, CYCLING, SWIMMING, CARDIO, STRENGTH -> true;
                default -> false;
            };
        }
    }

    private enum ExerciseCategory {
        WALKING("걷기", Set.of("walk", "walking", "걷")),
        RUNNING("러닝", Set.of("run", "running", "jog", "jogging", "track_running", "treadmill", "러닝", "조깅")),
        CYCLING("자전거", Set.of("cycle", "cycling", "bike", "biking", "stationary_biking", "indoor_bike", "자전거")),
        SWIMMING("수영", Set.of("swim", "swimming", "pool_swimming", "open_water_swimming", "수영")),
        CARDIO("유산소", Set.of("cardio", "aerobic", "aerobics", "elliptical", "jump_rope", "jumping_jacks", "burpees", "유산소")),
        STRENGTH("근력", Set.of("strength", "weight", "resistance", "bodyweight", "upper", "lower", "full_body", "core", "push_ups", "pull_ups", "sit_ups", "circuit_training", "mountain_climbers", "bench_press", "squats", "lunges", "leg_presses", "leg_extensions", "leg_curls", "back_extensions", "lat_pulldowns", "deadlifts", "shoulder_presses", "front_raises", "lateral_raises", "crunch", "leg_raises", "plank", "arm_curls", "arm_extensions", "weight_machine", "근력", "상체", "하체", "전신", "홈트")),
        YOGA("요가", Set.of("yoga", "pilates", "요가", "필라테스")),
        STRETCHING("스트레칭", Set.of("stretch", "stretching", "mobility", "스트레칭", "가동성")),
        RECOVERY("회복", Set.of("recovery", "회복")),
        GENERAL("운동", Set.of());

        private final String displayName;
        private final Set<String> keywords;

        ExerciseCategory(String displayName, Set<String> keywords) {
            this.displayName = displayName;
            this.keywords = keywords;
        }

        static ExerciseCategory from(UserQuest quest) {
            String text = questText(quest);
            for (ExerciseCategory category : values()) {
                if (category == GENERAL) {
                    continue;
                }
                if (category.keywords.stream().anyMatch(text::contains)) {
                    return category;
                }
            }
            if (UserQuest.TYPE_RECOVERY.equals(quest.getQuestType())) {
                return RECOVERY;
            }
            if (UserQuest.TYPE_OFF_DAY.equals(quest.getQuestType())) {
                return STRETCHING;
            }
            return GENERAL;
        }

        private static String questText(UserQuest quest) {
            StringBuilder builder = new StringBuilder();
            builder.append(quest.getQuestType()).append(' ')
                    .append(quest.getTargetMetric()).append(' ')
                    .append(quest.getTitle()).append(' ')
                    .append(quest.getDescription()).append(' ');
            RoutineSession sourceSession = quest.getSourceSession();
            if (sourceSession != null) {
                builder.append(sourceSession.getSessionName()).append(' ')
                        .append(sourceSession.getSessionType()).append(' ');
            }
            appendContext(builder, quest.getQuestContextJson());
            return builder.toString().toLowerCase(Locale.ROOT);
        }

        private static void appendContext(StringBuilder builder, Object value) {
            if (value instanceof Map<?, ?> map) {
                map.values().forEach(item -> appendContext(builder, item));
            } else if (value instanceof List<?> list) {
                list.forEach(item -> appendContext(builder, item));
            } else if (value != null) {
                builder.append(value).append(' ');
            }
        }

        String displayName() {
            return displayName;
        }

        Set<String> keywords() {
            return keywords.stream()
                    .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
