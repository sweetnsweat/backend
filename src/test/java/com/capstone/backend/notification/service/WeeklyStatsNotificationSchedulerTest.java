package com.capstone.backend.notification.service;

import com.capstone.backend.user.dto.WeeklyStatsResponse;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class WeeklyStatsNotificationSchedulerTest {

    @Autowired
    private WeeklyStatsNotificationScheduler scheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from user_push_tokens");
        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void sendsPreviousWeekStatsToRoutinePushCandidates() {
        User enabled = userRepository.save(User.createLocalUser("weeklySchedulerUser", "encoded-password", "weekly-on"));
        User disabled = userRepository.save(User.createLocalUser("weeklySchedulerDisabled", "encoded-password", "weekly-off"));
        jdbcTemplate.update("update users set push_routine_enabled = false where id = ?", disabled.getId());
        LocalDate weekStart = LocalDate.of(2026, 5, 11);
        seedCompletedQuest(enabled.getId(), weekStart, 30);
        seedCompletedQuest(enabled.getId(), weekStart.plusDays(2), 40);

        scheduler.sendWeeklyStatsReadyNotifications(weekStart);

        ArgumentCaptor<WeeklyStatsResponse> statsCaptor = ArgumentCaptor.forClass(WeeklyStatsResponse.class);
        verify(notificationService).sendWeeklyStatsReady(org.mockito.ArgumentMatchers.eq(enabled.getId()), statsCaptor.capture());
        verify(notificationService, never()).sendWeeklyStatsReady(org.mockito.ArgumentMatchers.eq(disabled.getId()), org.mockito.ArgumentMatchers.any());
        assertThat(statsCaptor.getValue().weekStartDate()).isEqualTo(weekStart);
        assertThat(statsCaptor.getValue().weekEndDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(statsCaptor.getValue().completedWorkoutCount()).isEqualTo(2);
        assertThat(statsCaptor.getValue().earnedExp()).isEqualTo(70);
    }

    private void seedCompletedQuest(Long userId, LocalDate questDate, int rewardExp) {
        jdbcTemplate.update("""
                insert into user_quests (
                    user_id,
                    quest_date,
                    quest_type,
                    target_metric,
                    title,
                    description,
                    target_value,
                    progress_value,
                    status,
                    condition_adjusted,
                    reward_currency,
                    reward_exp,
                    completed_at,
                    proof_json,
                    quest_context_json,
                    created_at
                )
                values (?, ?, 'routine', 'minutes', '주간 통계 테스트 퀘스트', '주간 통계 테스트용 완료 퀘스트입니다.', 10, 10, 'completed', false, 0, ?, CURRENT_TIMESTAMP, JSON '{}', JSON '{}', CURRENT_TIMESTAMP)
                """, userId, questDate, rewardExp);
    }
}
