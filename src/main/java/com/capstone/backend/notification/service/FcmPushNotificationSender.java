package com.capstone.backend.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.AndroidNotification.Priority;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Map;

public class FcmPushNotificationSender implements PushNotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushNotificationSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushSendResult send(List<String> tokens, PushNotificationMessage message) {
        if (tokens.isEmpty()) {
            return PushSendResult.sent(0, 0, 0);
        }

        MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(dataOrEmpty(message.data()))
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setPriority(Priority.HIGH)
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage);
            return PushSendResult.sent(tokens.size(), response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            return PushSendResult.sent(tokens.size(), 0, tokens.size());
        }
    }

    private Map<String, String> dataOrEmpty(Map<String, String> data) {
        return data == null ? Map.of() : data;
    }
}
