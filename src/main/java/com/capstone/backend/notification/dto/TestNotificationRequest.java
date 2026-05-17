package com.capstone.backend.notification.dto;

import jakarta.validation.constraints.Size;

public record TestNotificationRequest(
        @Size(max = 100)
        String title,

        @Size(max = 500)
        String body
) {
}
