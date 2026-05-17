package com.capstone.backend.notification.service;

import java.util.Map;

public record PushNotificationMessage(
        String title,
        String body,
        Map<String, String> data
) {
}
