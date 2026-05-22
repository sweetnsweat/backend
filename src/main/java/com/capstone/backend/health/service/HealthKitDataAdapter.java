package com.capstone.backend.health.service;

import com.capstone.backend.health.model.HealthDataSource;
import com.capstone.backend.health.model.HealthMetricType;
import org.springframework.stereotype.Component;

@Component
public class HealthKitDataAdapter implements PlatformHealthDataAdapter {

    @Override
    public boolean supports(HealthDataSource source) {
        return HealthDataSource.HEALTHKIT.equals(source);
    }

    @Override
    public HealthMetricType metricType(String rawRecordType) {
        return switch (rawRecordType == null ? "" : rawRecordType.trim()) {
            case "HKQuantityTypeIdentifierStepCount", "stepCount" -> HealthMetricType.STEPS;
            case "HKQuantityTypeIdentifierHeartRate", "heartRate" -> HealthMetricType.HEART_RATE;
            case "HKQuantityTypeIdentifierDistanceWalkingRunning", "distanceWalkingRunning" -> HealthMetricType.DISTANCE;
            case "HKQuantityTypeIdentifierActiveEnergyBurned", "activeEnergyBurned" -> HealthMetricType.ACTIVE_CALORIES_BURNED;
            case "HKQuantityTypeIdentifierBasalEnergyBurned", "basalEnergyBurned" -> HealthMetricType.BASAL_METABOLIC_RATE;
            case "HKQuantityTypeIdentifierBodyMass", "bodyMass" -> HealthMetricType.WEIGHT;
            case "HKQuantityTypeIdentifierHeight", "height" -> HealthMetricType.HEIGHT;
            case "HKCategoryTypeIdentifierSleepAnalysis", "sleepAnalysis" -> HealthMetricType.SLEEP_SESSION;
            case "HKQuantityTypeIdentifierVO2Max", "vo2Max" -> HealthMetricType.VO2_MAX;
            default -> null;
        };
    }
}
