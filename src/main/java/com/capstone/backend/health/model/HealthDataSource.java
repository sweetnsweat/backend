package com.capstone.backend.health.model;

import java.util.Locale;

public enum HealthDataSource {
    HEALTH_CONNECT,
    HEALTHKIT,
    MANUAL;

    public static HealthDataSource from(String value) {
        if (value == null || value.isBlank()) {
            return MANUAL;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if ("ANDROID".equals(normalized)) {
            return HEALTH_CONNECT;
        }
        if ("IOS".equals(normalized) || "APPLE_HEALTH".equals(normalized)) {
            return HEALTHKIT;
        }
        try {
            return HealthDataSource.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return MANUAL;
        }
    }
}
