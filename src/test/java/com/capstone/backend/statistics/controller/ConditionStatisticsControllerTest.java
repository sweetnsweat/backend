package com.capstone.backend.statistics.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConditionStatisticsControllerTest {

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
        jdbcTemplate.update("delete from user_push_tokens");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from user_favorite_exercises");
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routine_sessions");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void conditionStatisticsReturnsTrendSummaryExerciseEffectsAndInsight() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("statsUser", "encoded-password", "통계유저"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        LocalDate weekStart = KoreanTime.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        seedPreviousWeekConditions(user.getId(), weekStart);
        seedCondition(user.getId(), weekStart, new BigDecimal("80.00"), 3, 3);
        seedCondition(user.getId(), weekStart.plusDays(1), new BigDecimal("60.00"), 3, 4);
        seedCondition(user.getId(), weekStart.plusDays(2), new BigDecimal("80.00"), 4, 2);
        seedCondition(user.getId(), weekStart.plusDays(3), new BigDecimal("70.00"), 4, 3);
        seedCondition(user.getId(), weekStart.plusDays(4), new BigDecimal("90.00"), 5, 2);
        seedCondition(user.getId(), weekStart.plusDays(5), new BigDecimal("80.00"), 4, 2);
        seedCondition(user.getId(), weekStart.plusDays(6), new BigDecimal("80.00"), 4, 3);
        seedCompletedQuest(user.getId(), weekStart, "수영");
        seedCompletedQuest(user.getId(), weekStart.plusDays(2), "수영");
        seedCompletedQuest(user.getId(), weekStart.plusDays(4), "수영");
        seedCompletedQuest(user.getId(), weekStart.plusDays(5), "홈트");

        mockMvc.perform(get("/api/statistics/condition")
                        .queryParam("period", "week")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("week"))
                .andExpect(jsonPath("$.data.startDate").value(weekStart.toString()))
                .andExpect(jsonPath("$.data.endDate").value(weekStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.data.conditionTrend.length()").value(7))
                .andExpect(jsonPath("$.data.conditionTrend[0].dayLabel").value("월"))
                .andExpect(jsonPath("$.data.conditionTrend[0].condition").value(8.0))
                .andExpect(jsonPath("$.data.conditionTrend[0].energy").value(6))
                .andExpect(jsonPath("$.data.conditionTrend[0].stress").value(3))
                .andExpect(jsonPath("$.data.conditionTrend[0].exerciseLabel").value("수영"))
                .andExpect(jsonPath("$.data.summary.averageCondition").value(7.7))
                .andExpect(jsonPath("$.data.summary.workoutCount").value(4))
                .andExpect(jsonPath("$.data.summary.improvementRatePercent").value(54))
                .andExpect(jsonPath("$.data.exerciseEffects[0].displayName").value("수영"))
                .andExpect(jsonPath("$.data.exerciseEffects[0].averageCondition").value(8.3))
                .andExpect(jsonPath("$.data.exerciseEffects[0].sampleCount").value(3))
                .andExpect(jsonPath("$.data.aiInsight.summary").value(containsString("수영")))
                .andExpect(jsonPath("$.data.weeklyRecords.length()").value(7))
                .andExpect(jsonPath("$.data.weeklyRecords[5].exerciseLabel").value("홈트"));
    }

    @Test
    void conditionStatisticsRejectsInvalidPeriod() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("statsInvalidPeriod", "encoded-password", "통계유저"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(get("/api/statistics/condition")
                        .queryParam("period", "daily")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_PERIOD"))
                .andExpect(jsonPath("$.path").value("/api/statistics/condition"));
    }

    private void seedPreviousWeekConditions(Long userId, LocalDate currentWeekStart) {
        LocalDate previousWeekStart = currentWeekStart.minusDays(7);
        for (int i = 0; i < 7; i++) {
            seedCondition(userId, previousWeekStart.plusDays(i), new BigDecimal("50.00"), 3, 3);
        }
    }

    private void seedCondition(Long userId, LocalDate logDate, BigDecimal conditionScore, int energyLevel, int stressScore) {
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
                values (?, ?, 4, 3, ?, 2, ?, ?, 1.00, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, logDate, stressScore, energyLevel, conditionScore);
    }

    private void seedCompletedQuest(Long userId, LocalDate questDate, String exerciseType) {
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
                values (?, ?, 'routine', 'minutes', '완료 퀘스트', '완료된 테스트 퀘스트입니다.', 30, 30, 'completed', false, 30, 20, CURRENT_TIMESTAMP, JSON '{}', ? FORMAT JSON, CURRENT_TIMESTAMP)
                """, userId, questDate, "{\"exerciseType\":\"" + exerciseType + "\"}");
    }
}
