package com.capstone.backend.notification.service;

import java.util.List;

public interface PushNotificationSender {

    PushSendResult send(List<String> tokens, PushNotificationMessage message);
}
