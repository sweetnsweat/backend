package com.capstone.backend.health.service;

import com.capstone.backend.health.model.HealthDataSource;
import com.capstone.backend.health.model.HealthMetricType;

public interface PlatformHealthDataAdapter {

    boolean supports(HealthDataSource source);

    HealthMetricType metricType(String rawRecordType);
}
