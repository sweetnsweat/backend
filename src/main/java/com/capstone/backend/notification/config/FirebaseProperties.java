package com.capstone.backend.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(
        boolean enabled,
        String serviceAccountPath,
        String projectId
) {
}
