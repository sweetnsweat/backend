package com.capstone.backend.notification.dto;

public record NotificationSendResponse(
        boolean enabled,
        int targetCount,
        int successCount,
        int failureCount
) {
    public static NotificationSendResponse disabled(int targetCount) {
        return new NotificationSendResponse(false, targetCount, 0, targetCount);
    }

    public static NotificationSendResponse sent(int targetCount, int successCount, int failureCount) {
        return new NotificationSendResponse(true, targetCount, successCount, failureCount);
    }
}
