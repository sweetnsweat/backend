package com.capstone.backend.notification.service;

public record PushSendResult(
        boolean enabled,
        int targetCount,
        int successCount,
        int failureCount
) {
    public static PushSendResult disabled(int targetCount) {
        return new PushSendResult(false, targetCount, 0, targetCount);
    }

    public static PushSendResult sent(int targetCount, int successCount, int failureCount) {
        return new PushSendResult(true, targetCount, successCount, failureCount);
    }
}
