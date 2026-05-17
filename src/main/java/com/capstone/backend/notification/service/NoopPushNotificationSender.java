package com.capstone.backend.notification.service;

import java.util.List;

public class NoopPushNotificationSender implements PushNotificationSender {

    @Override
    public PushSendResult send(List<String> tokens, PushNotificationMessage message) {
        return PushSendResult.disabled(tokens.size());
    }
}
