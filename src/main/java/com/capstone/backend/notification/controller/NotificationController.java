package com.capstone.backend.notification.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.notification.dto.NotificationSendResponse;
import com.capstone.backend.notification.dto.PushTokenResponse;
import com.capstone.backend.notification.dto.RegisterPushTokenRequest;
import com.capstone.backend.notification.dto.TestNotificationRequest;
import com.capstone.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "푸시 알림", description = "FCM 토큰 등록과 테스트 푸시 발송 API")
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "FCM 토큰 등록", description = "앱에서 발급받은 FCM registration token을 현재 로그인 사용자에게 등록합니다.")
    @PostMapping("/push-tokens")
    public ApiResponse<PushTokenResponse> registerToken(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RegisterPushTokenRequest request
    ) {
        return ApiResponse.ok("푸시 토큰이 등록되었습니다.", notificationService.registerToken(authUser.userId(), request));
    }

    @Operation(summary = "FCM 토큰 비활성화", description = "로그아웃 또는 알림 해제 시 현재 사용자에게 등록된 푸시 토큰을 비활성화합니다.")
    @DeleteMapping("/push-tokens/{tokenId}")
    public ApiResponse<Void> disableToken(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long tokenId
    ) {
        notificationService.disableToken(authUser.userId(), tokenId);
        return ApiResponse.ok("푸시 토큰이 비활성화되었습니다.");
    }

    @Operation(summary = "테스트 푸시 발송", description = "현재 로그인 사용자의 활성 FCM 토큰으로 테스트 푸시 알림을 발송합니다.")
    @PostMapping("/notifications/test")
    public ApiResponse<NotificationSendResponse> sendTestNotification(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TestNotificationRequest request
    ) {
        return ApiResponse.ok("테스트 푸시 발송 요청을 처리했습니다.", notificationService.sendTestNotification(authUser.userId(), request));
    }
}
