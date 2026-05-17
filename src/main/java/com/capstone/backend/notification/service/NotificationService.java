package com.capstone.backend.notification.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.notification.dto.NotificationSendResponse;
import com.capstone.backend.notification.dto.PushTokenResponse;
import com.capstone.backend.notification.dto.RegisterPushTokenRequest;
import com.capstone.backend.notification.dto.TestNotificationRequest;
import com.capstone.backend.notification.entity.UserPushToken;
import com.capstone.backend.notification.repository.UserPushTokenRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final UserPushTokenRepository userPushTokenRepository;
    private final PushNotificationSender pushNotificationSender;

    public NotificationService(UserRepository userRepository,
                               UserPushTokenRepository userPushTokenRepository,
                               PushNotificationSender pushNotificationSender) {
        this.userRepository = userRepository;
        this.userPushTokenRepository = userPushTokenRepository;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Transactional
    public PushTokenResponse registerToken(Long userId, RegisterPushTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String token = request.token().trim();
        String platform = request.platform().trim().toLowerCase(Locale.ROOT);
        String deviceId = normalize(request.deviceId());

        UserPushToken pushToken = userPushTokenRepository.findByToken(token)
                .orElseGet(() -> UserPushToken.register(user, token, platform, deviceId));
        pushToken.updateRegistration(user, platform, deviceId);

        return PushTokenResponse.from(userPushTokenRepository.save(pushToken));
    }

    @Transactional
    public void disableToken(Long userId, Long tokenId) {
        UserPushToken pushToken = userPushTokenRepository.findByIdAndUser_Id(tokenId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PUSH_TOKEN_NOT_FOUND", "등록된 푸시 토큰을 찾을 수 없습니다."));

        pushToken.disable();
    }

    @Transactional(readOnly = true)
    public NotificationSendResponse sendTestNotification(Long userId, TestNotificationRequest request) {
        List<String> tokens = userPushTokenRepository.findByUser_IdAndEnabledTrue(userId).stream()
                .map(UserPushToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return NotificationSendResponse.sent(0, 0, 0);
        }

        PushNotificationMessage message = new PushNotificationMessage(
                textOrDefault(request.title(), "Sweet & Sweat 테스트 알림"),
                textOrDefault(request.body(), "푸시 알림 연동이 정상 동작하는지 확인합니다."),
                Map.of("type", "test")
        );
        PushSendResult result = pushNotificationSender.send(tokens, message);
        if (!result.enabled()) {
            return NotificationSendResponse.disabled(result.targetCount());
        }
        return NotificationSendResponse.sent(result.targetCount(), result.successCount(), result.failureCount());
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String textOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null ? defaultValue : normalized;
    }
}
