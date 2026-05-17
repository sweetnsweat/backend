package com.capstone.backend.notification.dto;

import com.capstone.backend.notification.entity.UserPushToken;
import java.time.Instant;

public record PushTokenResponse(
        Long id,
        String platform,
        String deviceId,
        boolean enabled,
        Instant lastSeenAt
) {
    public static PushTokenResponse from(UserPushToken pushToken) {
        return new PushTokenResponse(
                pushToken.getId(),
                pushToken.getPlatform(),
                pushToken.getDeviceId(),
                pushToken.isEnabled(),
                pushToken.getLastSeenAt()
        );
    }
}
