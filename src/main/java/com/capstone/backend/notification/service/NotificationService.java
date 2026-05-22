package com.capstone.backend.notification.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.battle.entity.Battle;
import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.notification.dto.NotificationSendResponse;
import com.capstone.backend.notification.dto.PushTokenResponse;
import com.capstone.backend.notification.dto.RegisterPushTokenRequest;
import com.capstone.backend.notification.dto.TestNotificationRequest;
import com.capstone.backend.notification.entity.UserPushToken;
import com.capstone.backend.notification.repository.UserPushTokenRepository;
import com.capstone.backend.user.dto.WeeklyStatsResponse;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
                Map.of("type", NotificationType.TEST.name())
        );
        return send(tokens, message);
    }

    @Transactional(readOnly = true)
    public NotificationSendResponse sendBattleMatched(Battle battle, List<BattleParticipant> participants) {
        List<Long> userIds = participantUserIds(participants);
        List<String> tokens = competitionTokens(userIds);
        PushNotificationMessage message = new PushNotificationMessage(
                "배틀 매칭 완료",
                "상대가 정해졌어요. 지금 배틀을 확인해보세요.",
                Map.of(
                        "type", NotificationType.BATTLE_MATCHED.name(),
                        "route", "battle/detail",
                        "battleId", String.valueOf(battle.getId()),
                        "battleMode", battle.getMode().name()
                )
        );
        return send(tokens, message);
    }

    @Transactional(readOnly = true)
    public NotificationSendResponse sendBattleResultReady(Battle battle, List<BattleParticipant> participants) {
        NotificationSendResponse total = NotificationSendResponse.sent(0, 0, 0);
        for (BattleParticipant participant : participants) {
            List<String> tokens = competitionTokens(List.of(participant.getUser().getId()));
            PushNotificationMessage message = new PushNotificationMessage(
                    "배틀 결과 확정",
                    battleResultBody(participant),
                    Map.of(
                            "type", NotificationType.BATTLE_RESULT_READY.name(),
                            "route", "battle/result",
                            "battleId", String.valueOf(battle.getId()),
                            "battleMode", battle.getMode().name(),
                            "result", participant.getResult().name()
                    )
            );
            total = merge(total, send(tokens, message));
        }
        return total;
    }

    @Transactional(readOnly = true)
    public NotificationSendResponse sendWeeklyStatsReady(Long userId, WeeklyStatsResponse stats) {
        List<String> tokens = userPushTokenRepository.findRoutineEnabledTokensByUserId(userId).stream()
                .map(UserPushToken::getToken)
                .toList();
        PushNotificationMessage message = new PushNotificationMessage(
                "지난주 운동 통계가 준비됐어요",
                "완료 운동 %d회, 획득 EXP %d를 확인해보세요.".formatted(stats.completedWorkoutCount(), stats.earnedExp()),
                Map.of(
                        "type", NotificationType.WEEKLY_STATS_READY.name(),
                        "route", "stats/weekly",
                        "weekStartDate", stats.weekStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "weekEndDate", stats.weekEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
        );
        return send(tokens, message);
    }

    private List<Long> participantUserIds(List<BattleParticipant> participants) {
        return participants.stream()
                .map(BattleParticipant::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> competitionTokens(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userPushTokenRepository.findCompetitionEnabledTokensByUserIds(userIds).stream()
                .map(UserPushToken::getToken)
                .toList();
    }

    private NotificationSendResponse send(List<String> tokens, PushNotificationMessage message) {
        if (tokens.isEmpty()) {
            return NotificationSendResponse.sent(0, 0, 0);
        }
        PushSendResult result = pushNotificationSender.send(tokens, message);
        if (!result.enabled()) {
            return NotificationSendResponse.disabled(result.targetCount());
        }
        return NotificationSendResponse.sent(result.targetCount(), result.successCount(), result.failureCount());
    }

    private NotificationSendResponse merge(NotificationSendResponse left, NotificationSendResponse right) {
        int targetCount = left.targetCount() + right.targetCount();
        int successCount = left.successCount() + right.successCount();
        int failureCount = left.failureCount() + right.failureCount();
        if (!left.enabled() || !right.enabled()) {
            return new NotificationSendResponse(false, targetCount, successCount, failureCount);
        }
        return NotificationSendResponse.sent(targetCount, successCount, failureCount);
    }

    private String battleResultBody(BattleParticipant participant) {
        return switch (participant.getResult()) {
            case WIN -> "배틀에서 승리했어요. 결과를 확인해보세요.";
            case LOSS -> "배틀 결과가 나왔어요. 다음 도전을 준비해보세요.";
            case DRAW -> "배틀이 무승부로 끝났어요. 결과를 확인해보세요.";
            case PENDING -> "배틀 결과가 준비됐어요. 지금 확인해보세요.";
        };
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
