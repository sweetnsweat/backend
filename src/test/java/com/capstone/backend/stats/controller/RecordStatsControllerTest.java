package com.capstone.backend.stats.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecordStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from battle_match_queue");
        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from user_favorite_exercises");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from health_daily_summaries");
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routine_sessions");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void statsReturnsRecordPageDataFromConditionsHealthAndCompletedQuests() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser user = testUser("recordStatsUser", "기록통계유저");
        LocalDate today = KoreanTime.today();
        seedCondition(user.userId(), today.minusDays(2), 3, 3, 4, 2, 60);
        seedCondition(user.userId(), today.minusDays(1), 4, 4, 3, 3, 70);
        seedCondition(user.userId(), today, 5, 5, 2, 5, 80);
        seedHealthDailySummary(user.userId(), today, 5000, 3200, 220, 35);
        seedCompletedQuest(user.userId(), today, """
                {
                  "exerciseName": "running"
                }
                """);

        mockMvc.perform(get("/api/records/stats")
                        .param("period", "WEEKLY")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("기록 통계를 조회했습니다."))
                .andExpect(jsonPath("$.data.period").value("WEEKLY"))
                .andExpect(jsonPath("$.data.conditionTrend.length()").value(7))
                .andExpect(jsonPath("$.data.summary.averageConditionScore").value(70.0))
                .andExpect(jsonPath("$.data.summary.exerciseCount").value(1))
                .andExpect(jsonPath("$.data.summary.completedQuestCount").value(1))
                .andExpect(jsonPath("$.data.summary.healthSyncedDays").value(1))
                .andExpect(jsonPath("$.data.summary.totalExerciseMinutes").value(35))
                .andExpect(jsonPath("$.data.summary.totalSteps").value(5000))
                .andExpect(jsonPath("$.data.summary.totalDistanceMeters").value(3200))
                .andExpect(jsonPath("$.data.summary.totalActiveCaloriesKcal").value(220))
                .andExpect(jsonPath("$.data.exerciseEffects[0].exerciseType").value("running"))
                .andExpect(jsonPath("$.data.exerciseEffects[0].label").value("러닝"))
                .andExpect(jsonPath("$.data.exerciseEffects[0].completedCount").value(1))
                .andExpect(jsonPath("$.data.exerciseEffects[0].exerciseMinutes").value(35))
                .andExpect(jsonPath("$.data.dailyRecords[*].exerciseLabel").value(hasItem("러닝")))
                .andExpect(jsonPath("$.data.dailyRecords[*].completedQuestCount").value(hasItem(1)))
                .andExpect(jsonPath("$.data.insight.title").value("AI 분석 인사이트"));
    }

    @Test
    void statsDefaultsToWeeklyAndReturnsEmptyShapeWhenNoDataExists() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser user = testUser("emptyRecordStatsUser", "빈통계유저");

        mockMvc.perform(get("/api/records/stats")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("WEEKLY"))
                .andExpect(jsonPath("$.data.conditionTrend.length()").value(7))
                .andExpect(jsonPath("$.data.summary.exerciseCount").value(0))
                .andExpect(jsonPath("$.data.summary.totalSteps").value(0))
                .andExpect(jsonPath("$.data.exerciseEffects.length()").value(0))
                .andExpect(jsonPath("$.data.insight.title").value("분석할 기록이 부족합니다."));
    }

    @Test
    void statsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/records/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private TestUser testUser(String loginId, String nickname) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", nickname));
        return new TestUser(user.getId(), jwtTokenService.issueTokenPair(user).accessToken());
    }

    private void seedCondition(Long userId,
                               LocalDate logDate,
                               int conditionLevel,
                               int sleepScore,
                               int stressScore,
                               int energyLevel,
                               int conditionScore) {
        int fatigueScore = 6 - energyLevel;
        jdbcTemplate.update("""
                insert into condition_logs (
                    user_id,
                    log_date,
                    condition_level,
                    sleep_score,
                    stress_score,
                    fatigue_score,
                    energy_level,
                    condition_score,
                    exercise_multiplier,
                    fatigue,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                userId,
                logDate,
                conditionLevel,
                sleepScore,
                stressScore,
                fatigueScore,
                energyLevel,
                BigDecimal.valueOf(conditionScore),
                BigDecimal.ONE,
                fatigueScore
        );
    }

    private void seedHealthDailySummary(Long userId,
                                        LocalDate summaryDate,
                                        int steps,
                                        int distanceMeters,
                                        int activeCalories,
                                        int exerciseMinutes) {
        jdbcTemplate.update("""
                insert into health_daily_summaries (
                    user_id,
                    summary_date,
                    steps,
                    distance_meters,
                    active_calories_kcal,
                    exercise_minutes,
                    sample_count,
                    synced_at,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, ?, ?, ?, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, summaryDate, steps, distanceMeters, activeCalories, exerciseMinutes);
    }

    private void seedCompletedQuest(Long userId, LocalDate questDate, String questContextJson) {
        String escapedQuestContextJson = questContextJson.replace("'", "''");
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
                values (?, ?, 'routine', 'minutes', '통계 테스트 퀘스트', '기록 통계 테스트용 완료 퀘스트입니다.', 30, 30, 'completed', false, 0, 0, CURRENT_TIMESTAMP, JSON '{}', JSON '""" + escapedQuestContextJson + """
                ', CURRENT_TIMESTAMP)
                """, userId, questDate);
    }

    private record TestUser(Long userId, String accessToken) {
    }
}
