package com.capstone.backend.health.model;

import java.util.Locale;

public enum HealthMetricType {
    STEPS,
    HEART_RATE,
    DISTANCE,
    EXERCISE_SESSION,
    TOTAL_CALORIES_BURNED,
    ACTIVE_CALORIES_BURNED,
    BASAL_METABOLIC_RATE,
    BLOOD_GLUCOSE,
    BLOOD_PRESSURE,
    BODY_FAT,
    HEIGHT,
    NUTRITION,
    OXYGEN_SATURATION,
    POWER,
    SLEEP_SESSION,
    SPEED,
    VO2_MAX,
    WEIGHT;

    public static HealthMetricType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "STEP", "STEP_COUNT" -> STEPS;
            case "HEARTRATE", "HEART_RATE_BPM" -> HEART_RATE;
            case "CALORIES", "TOTAL_CALORIES", "TOTAL_CALORIES_BURNED" -> TOTAL_CALORIES_BURNED;
            case "EXERCISE", "WORKOUT", "WORKOUT_SESSION" -> EXERCISE_SESSION;
            case "SLEEP" -> SLEEP_SESSION;
            case "VO2MAX", "VO2_MAX" -> VO2_MAX;
            default -> {
                try {
                    yield HealthMetricType.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }
}
