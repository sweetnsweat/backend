package com.capstone.backend.health.service;

import com.capstone.backend.health.model.HealthDataSource;
import com.capstone.backend.health.model.HealthMetricType;
import org.springframework.stereotype.Component;

@Component
public class HealthConnectDataAdapter implements PlatformHealthDataAdapter {

    @Override
    public boolean supports(HealthDataSource source) {
        return HealthDataSource.HEALTH_CONNECT.equals(source);
    }

    @Override
    public HealthMetricType metricType(String rawRecordType) {
        return switch (rawRecordType == null ? "" : rawRecordType.trim()) {
            case "BasalMetabolicRate" -> HealthMetricType.BASAL_METABOLIC_RATE;
            case "BloodGlucose" -> HealthMetricType.BLOOD_GLUCOSE;
            case "BloodPressure" -> HealthMetricType.BLOOD_PRESSURE;
            case "BodyFat" -> HealthMetricType.BODY_FAT;
            case "ActiveCaloriesBurned" -> HealthMetricType.ACTIVE_CALORIES_BURNED;
            case "Distance" -> HealthMetricType.DISTANCE;
            case "ExerciseSession" -> HealthMetricType.EXERCISE_SESSION;
            case "HeartRate" -> HealthMetricType.HEART_RATE;
            case "Height" -> HealthMetricType.HEIGHT;
            case "Nutrition" -> HealthMetricType.NUTRITION;
            case "OxygenSaturation" -> HealthMetricType.OXYGEN_SATURATION;
            case "Power" -> HealthMetricType.POWER;
            case "SleepSession" -> HealthMetricType.SLEEP_SESSION;
            case "Speed" -> HealthMetricType.SPEED;
            case "Steps" -> HealthMetricType.STEPS;
            case "TotalCaloriesBurned" -> HealthMetricType.TOTAL_CALORIES_BURNED;
            case "Vo2Max" -> HealthMetricType.VO2_MAX;
            case "Weight" -> HealthMetricType.WEIGHT;
            default -> null;
        };
    }
}
