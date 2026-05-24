package com.capstone.backend.notification.service;

import com.capstone.backend.battle.entity.Battle;
import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.battle.entity.BattleResult;
import com.capstone.backend.battle.repository.BattleParticipantRepository;
import com.capstone.backend.battle.repository.BattleRepository;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.notification.entity.UserPushToken;
import com.capstone.backend.notification.repository.UserPushTokenRepository;
import com.capstone.backend.user.dto.WeeklyStatsResponse;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPushTokenRepository userPushTokenRepository;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private PushNotificationSender pushNotificationSender;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from user_push_tokens");
        jdbcTemplate.update("delete from battle_match_queue");

        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void battleMatchedUsesCompetitionPreferenceAndBattleRoutePayload() {
        when(pushNotificationSender.send(anyList(), any(PushNotificationMessage.class)))
                .thenAnswer(invocation -> {
                    List<?> tokens = invocation.getArgument(0);
                    return PushSendResult.sent(tokens.size(), tokens.size(), 0);
                });
        User enabled = userRepository.save(User.createLocalUser("battlePushEnabled", "encoded-password", "enabled"));
        User disabled = userRepository.save(User.createLocalUser("battlePushDisabled", "encoded-password", "disabled"));
        userPushTokenRepository.save(UserPushToken.register(enabled, "enabled-token", "android", null));
        userPushTokenRepository.save(UserPushToken.register(disabled, "disabled-token", "android", null));
        jdbcTemplate.update("update users set push_competition_enabled = false where id = ?", disabled.getId());

        Battle battle = battleRepository.save(Battle.create(
                BattleMode.DAILY,
                KoreanTime.today(),
                KoreanTime.today(),
                KoreanTime.today().atStartOfDay(KoreanTime.ZONE_ID).toInstant(),
                KoreanTime.today().plusDays(1).atStartOfDay(KoreanTime.ZONE_ID).toInstant()
        ));
        List<BattleParticipant> participants = battleParticipantRepository.saveAll(List.of(
                BattleParticipant.join(battle, enabled),
                BattleParticipant.join(battle, disabled)
        ));

        notificationService.sendBattleMatched(battle, participants);

        ArgumentCaptor<PushNotificationMessage> messageCaptor = ArgumentCaptor.forClass(PushNotificationMessage.class);
        verify(pushNotificationSender).send(eq(List.of("enabled-token")), messageCaptor.capture());
        PushNotificationMessage message = messageCaptor.getValue();
        assertThat(message.data()).containsEntry("type", NotificationType.BATTLE_MATCHED.name());
        assertThat(message.data()).containsEntry("route", "battle/detail");
        assertThat(message.data()).containsEntry("battleId", String.valueOf(battle.getId()));
        assertThat(message.data()).containsEntry("battleMode", "DAILY");
    }

    @Test
    void battleResultReadySendsPersonalResultPayload() {
        when(pushNotificationSender.send(anyList(), any(PushNotificationMessage.class)))
                .thenAnswer(invocation -> {
                    List<?> tokens = invocation.getArgument(0);
                    return PushSendResult.sent(tokens.size(), tokens.size(), 0);
                });
        User winner = userRepository.save(User.createLocalUser("battleWinnerPush", "encoded-password", "winner"));
        userPushTokenRepository.save(UserPushToken.register(winner, "winner-token", "android", null));
        Battle battle = battleRepository.save(Battle.create(
                BattleMode.WEEKLY,
                KoreanTime.today(),
                KoreanTime.today().plusDays(6),
                KoreanTime.today().atStartOfDay(KoreanTime.ZONE_ID).toInstant(),
                KoreanTime.today().plusDays(7).atStartOfDay(KoreanTime.ZONE_ID).toInstant()
        ));
        BattleParticipant participant = BattleParticipant.join(battle, winner);
        participant.finalizeResult(100, BattleResult.WIN);
        List<BattleParticipant> participants = battleParticipantRepository.saveAll(List.of(participant));

        notificationService.sendBattleResultReady(battle, participants);

        ArgumentCaptor<PushNotificationMessage> messageCaptor = ArgumentCaptor.forClass(PushNotificationMessage.class);
        verify(pushNotificationSender).send(eq(List.of("winner-token")), messageCaptor.capture());
        assertThat(messageCaptor.getValue().data()).containsEntry("type", NotificationType.BATTLE_RESULT_READY.name());
        assertThat(messageCaptor.getValue().data()).containsEntry("route", "battle/result");
        assertThat(messageCaptor.getValue().data()).containsEntry("result", "WIN");
    }

    @Test
    void weeklyStatsReadyUsesRoutinePreferenceAndStatsRoutePayload() {
        when(pushNotificationSender.send(anyList(), any(PushNotificationMessage.class)))
                .thenReturn(PushSendResult.sent(1, 1, 0));
        User user = userRepository.save(User.createLocalUser("weeklyPushUser", "encoded-password", "weekly"));
        userPushTokenRepository.save(UserPushToken.register(user, "weekly-token", "android", null));
        WeeklyStatsResponse stats = new WeeklyStatsResponse(
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 17),
                4,
                3,
                500,
                120
        );

        notificationService.sendWeeklyStatsReady(user.getId(), stats);

        ArgumentCaptor<PushNotificationMessage> messageCaptor = ArgumentCaptor.forClass(PushNotificationMessage.class);
        verify(pushNotificationSender).send(eq(List.of("weekly-token")), messageCaptor.capture());
        assertThat(messageCaptor.getValue().data()).containsEntry("type", NotificationType.WEEKLY_STATS_READY.name());
        assertThat(messageCaptor.getValue().data()).containsEntry("route", "stats/weekly");
        assertThat(messageCaptor.getValue().data()).containsEntry("weekStartDate", "2026-05-11");
        assertThat(messageCaptor.getValue().body()).contains("완료 운동 4회", "획득 EXP 120");
    }

    @Test
    void weeklyStatsReadySkipsRoutineDisabledUser() {
        User user = userRepository.save(User.createLocalUser("weeklyPushDisabledUser", "encoded-password", "weekly-off"));
        userPushTokenRepository.save(UserPushToken.register(user, "weekly-disabled-token", "android", null));
        jdbcTemplate.update("update users set push_routine_enabled = false where id = ?", user.getId());

        notificationService.sendWeeklyStatsReady(user.getId(), new WeeklyStatsResponse(
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 17),
                0,
                0,
                0,
                0
        ));

        verify(pushNotificationSender, never()).send(anyList(), any(PushNotificationMessage.class));
    }
}
