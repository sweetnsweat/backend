package com.capstone.backend.quest.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuestControllerTest {

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
        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from user_favorite_exercises");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routine_sessions");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void todayQuestCreatesRoutineQuestAndReusesItOnSameDay() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questRoutineUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questType").value("ROUTINE"))
                .andExpect(jsonPath("$.data.targetMetric").value("ROUTINE"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.targetValue").value(1))
                .andExpect(jsonPath("$.data.routineId").value(routineId))
                .andExpect(jsonPath("$.data.sessionName").value("오늘 세션"))
                .andExpect(jsonPath("$.data.sessionType").value("full_body"))
                .andExpect(jsonPath("$.data.sessionTypeDisplayName").value("전신"))
                .andExpect(jsonPath("$.data.rewardExp").value(30))
                .andExpect(jsonPath("$.data.rewardCurrency").value(15))
                .andExpect(jsonPath("$.data.rewardGold").value(15))
                .andExpect(jsonPath("$.data.exercises.length()").value(3))
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Quest Exercise 1"))
                .andExpect(jsonPath("$.data.exercises[2].exerciseName").value("Quest Exercise 3"));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questType").value("ROUTINE"))
                .andExpect(jsonPath("$.data.targetValue").value(1));

        Integer questCount = jdbcTemplate.queryForObject(
                "select count(*) from user_quests where user_id = ? and quest_date = ?",
                Integer.class,
                testUser.userId(),
                KoreanTime.today()
        );
        org.assertj.core.api.Assertions.assertThat(questCount).isEqualTo(1);
    }

    @Test
    void todayQuestByUserIdAllowsAiServerWithoutBearerToken() throws Exception {
        TestUser testUser = onboardedUser("questAiUserIdUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today/by-user")
                        .queryParam("userId", testUser.userId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questType").value("ROUTINE"))
                .andExpect(jsonPath("$.data.routineId").value(routineId))
                .andExpect(jsonPath("$.data.exercises.length()").value(3));
    }

    @Test
    void todayQuestCreatesOffDayQuestWhenTodayHasNoRoutineSession() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questOffDayUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().plusDays(1).getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questType").value("OFF_DAY"))
                .andExpect(jsonPath("$.data.targetMetric").value("MINUTES"))
                .andExpect(jsonPath("$.data.targetValue").value(15))
                .andExpect(jsonPath("$.data.rewardExp").value(15))
                .andExpect(jsonPath("$.data.rewardCurrency").value(10))
                .andExpect(jsonPath("$.data.rewardGold").value(10))
                .andExpect(jsonPath("$.data.sessionType").value(nullValue()))
                .andExpect(jsonPath("$.data.sessionTypeDisplayName").value(nullValue()))
                .andExpect(jsonPath("$.data.exercises").isEmpty());
    }

    @Test
    void todayQuestCreatesRecoveryQuestWhenConditionIsLow() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questRecoveryUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(35.00), BigDecimal.valueOf(0.70));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questType").value("RECOVERY"))
                .andExpect(jsonPath("$.data.targetMetric").value("MINUTES"))
                .andExpect(jsonPath("$.data.targetValue").value(10))
                .andExpect(jsonPath("$.data.rewardExp").value(10))
                .andExpect(jsonPath("$.data.rewardCurrency").value(5))
                .andExpect(jsonPath("$.data.rewardGold").value(5))
                .andExpect(jsonPath("$.data.conditionAdjusted").value(true));
    }

    @Test
    void todayQuestRequiresConditionInput() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questConditionRequiredUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONDITION_REQUIRED"));
    }

    @Test
    void completeQuestMarksTodayQuestCompleted() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questCompleteUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk());
        Long questId = jdbcTemplate.queryForObject(
                "select id from user_quests where user_id = ? and quest_date = ?",
                Long.class,
                testUser.userId(),
                KoreanTime.today()
        );

        mockMvc.perform(patch("/api/quests/{questId}/complete", questId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "progressValue": 2,
                                  "proof": {
                                    "source": "manual"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completionType").value("MANUAL"))
                .andExpect(jsonPath("$.data.verificationStatus").value("NOT_PROVIDED"))
                .andExpect(jsonPath("$.data.battleEligible").value(false))
                .andExpect(jsonPath("$.data.rewardExp").value(10))
                .andExpect(jsonPath("$.data.rewardGold").value(5))
                .andExpect(jsonPath("$.data.progressValue").value(2));

        String manualProofJson = jdbcTemplate.queryForObject("select proof_json from user_quests where id = ?", String.class, questId);
        org.assertj.core.api.Assertions.assertThat(manualProofJson).contains("\"completionType\":\"MANUAL\"");
        org.assertj.core.api.Assertions.assertThat(manualProofJson).contains("\"battleEligible\":false");
        assertQuestCompletionReward(testUser.userId(), questId, 10, 5);

        mockMvc.perform(patch("/api/quests/{questId}/complete", questId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "progressValue": 2,
                                  "proof": {
                                    "source": "manual"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertQuestCompletionReward(testUser.userId(), questId, 10, 5);

        mockMvc.perform(post("/api/quests/{questId}/reset", questId)
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.completionType").value(nullValue()))
                .andExpect(jsonPath("$.data.verificationStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.battleEligible").value(nullValue()))
                .andExpect(jsonPath("$.data.rewardExp").value(30))
                .andExpect(jsonPath("$.data.rewardGold").value(15))
                .andExpect(jsonPath("$.data.progressValue").value(0))
                .andExpect(jsonPath("$.data.completedAt").value(nullValue()));

        String resetStatus = jdbcTemplate.queryForObject("select status from user_quests where id = ?", String.class, questId);
        String resetProofJson = jdbcTemplate.queryForObject("select proof_json from user_quests where id = ?", String.class, questId);
        Integer resetProgressValue = jdbcTemplate.queryForObject("select progress_value from user_quests where id = ?", Integer.class, questId);
        org.assertj.core.api.Assertions.assertThat(resetStatus).isEqualTo("issued");
        org.assertj.core.api.Assertions.assertThat(resetProofJson).isEqualTo("{}");
        org.assertj.core.api.Assertions.assertThat(resetProgressValue).isZero();
        assertNoQuestCompletionReward(testUser.userId(), questId);
    }

    @Test
    void completeQuestAcceptsHealthProofInsideVerificationWindow() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questHealthProofUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationWindow.startTime").exists());
        Long questId = jdbcTemplate.queryForObject(
                "select id from user_quests where user_id = ? and quest_date = ?",
                Long.class,
                testUser.userId(),
                KoreanTime.today()
        );
        Instant now = KoreanTime.nowInstant();
        Instant questCreatedAt = now.minusSeconds(30 * 60L);
        jdbcTemplate.update("update user_quests set created_at = ? where id = ?", questCreatedAt, questId);
        Instant exerciseStart = now.minusSeconds(26 * 60L);

        mockMvc.perform(patch("/api/quests/{questId}/complete", questId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "healthSamples": [
                                    {
                                      "type": "ExerciseSession",
                                      "value": 26,
                                      "unit": "minutes",
                                      "startTime": "%s",
                                      "endTime": "%s",
                                      "source": "health_connect",
                                      "dataOrigin": "com.sec.android.app.shealth",
                                      "rawRecordType": "StrengthTraining"
                                    },
                                    {
                                      "type": "ActiveCaloriesBurned",
                                      "value": 160,
                                      "unit": "kcal",
                                      "startTime": "%s",
                                      "endTime": "%s",
                                      "source": "health_connect",
                                      "dataOrigin": "com.sec.android.app.shealth",
                                      "rawRecordType": "ActiveCaloriesBurned"
                                    },
                                    {
                                      "type": "HeartRate",
                                      "value": 128,
                                      "unit": "bpm",
                                      "startTime": "%s",
                                      "endTime": "%s",
                                      "source": "health_connect",
                                      "dataOrigin": "com.sec.android.app.shealth",
                                      "rawRecordType": "HeartRate"
                                    }
                                  ]
                                }
                                """.formatted(exerciseStart, now, exerciseStart, now, exerciseStart, now)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completionType").value("VERIFIED"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.battleEligible").value(true))
                .andExpect(jsonPath("$.data.rewardExp").value(30))
                .andExpect(jsonPath("$.data.rewardGold").value(15))
                .andExpect(jsonPath("$.data.progressValue").value(1));

        String proofJson = jdbcTemplate.queryForObject("select proof_json from user_quests where id = ?", String.class, questId);
        org.assertj.core.api.Assertions.assertThat(proofJson).contains("\"verified\":true");
        org.assertj.core.api.Assertions.assertThat(proofJson).contains("\"rule\":\"strength_health_proof\"");
        assertQuestCompletionReward(testUser.userId(), questId, 30, 15);
    }

    @Test
    void completeQuestFallsBackToManualWhenHealthProofIsInsufficient() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questOldHealthProofUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk());
        Long questId = jdbcTemplate.queryForObject(
                "select id from user_quests where user_id = ? and quest_date = ?",
                Long.class,
                testUser.userId(),
                KoreanTime.today()
        );
        Instant now = KoreanTime.nowInstant();
        Instant oldExerciseStart = now.minusSeconds(90 * 60L);
        Instant oldExerciseEnd = now.minusSeconds(60 * 60L);

        mockMvc.perform(patch("/api/quests/{questId}/complete", questId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "healthSamples": [
                                    {
                                      "type": "ExerciseSession",
                                      "value": 30,
                                      "unit": "minutes",
                                      "startTime": "%s",
                                      "endTime": "%s",
                                      "source": "health_connect",
                                      "dataOrigin": "com.sec.android.app.shealth",
                                      "rawRecordType": "StrengthTraining"
                                    }
                                  ]
                }
                """.formatted(oldExerciseStart, oldExerciseEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completionType").value("MANUAL"))
                .andExpect(jsonPath("$.data.verificationStatus").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.data.battleEligible").value(false))
                .andExpect(jsonPath("$.data.rewardExp").value(10))
                .andExpect(jsonPath("$.data.rewardGold").value(5));

        String status = jdbcTemplate.queryForObject("select status from user_quests where id = ?", String.class, questId);
        org.assertj.core.api.Assertions.assertThat(status).isEqualTo("completed");
        String proofJson = jdbcTemplate.queryForObject("select proof_json from user_quests where id = ?", String.class, questId);
        org.assertj.core.api.Assertions.assertThat(proofJson).contains("\"completionType\":\"MANUAL\"");
        org.assertj.core.api.Assertions.assertThat(proofJson).contains("\"verificationStatus\":\"INSUFFICIENT_DATA\"");
        org.assertj.core.api.Assertions.assertThat(proofJson).contains("\"battleEligible\":false");
        assertQuestCompletionReward(testUser.userId(), questId, 10, 5);
    }

    @Test
    void completeQuestFallsBackToManualWhenHealthSampleCannotBeParsed() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questBadHealthSampleUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk());
        Long questId = jdbcTemplate.queryForObject(
                "select id from user_quests where user_id = ? and quest_date = ?",
                Long.class,
                testUser.userId(),
                KoreanTime.today()
        );

        mockMvc.perform(patch("/api/quests/{questId}/complete", questId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "healthSamples": [
                                    {
                                      "type": "ExerciseSession",
                                      "unit": "minutes",
                                      "source": "health_connect",
                                      "rawRecordType": "StrengthTraining"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completionType").value("MANUAL"))
                .andExpect(jsonPath("$.data.verificationStatus").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.data.battleEligible").value(false))
                .andExpect(jsonPath("$.data.rewardExp").value(10))
                .andExpect(jsonPath("$.data.rewardGold").value(5));

        String proofJson = jdbcTemplate.queryForObject("select proof_json from user_quests where id = ?", String.class, questId);
        org.assertj.core.api.Assertions.assertThat(proofJson).contains("\"failureCode\":\"HEALTH_VALUE_REQUIRED\"");
        assertQuestCompletionReward(testUser.userId(), questId, 10, 5);
    }

    @Test
    void yesterdayUnfinishedQuestExpiresWhenTodayQuestIsRequested() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = onboardedUser("questExpireUser");
        Long routineId = seedRoutineWithSession(KoreanTime.today().getDayOfWeek().name());
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, testUser.userId());
        seedCondition(testUser.userId(), KoreanTime.today(), BigDecimal.valueOf(72.92), BigDecimal.valueOf(1.00));
        seedOldQuest(testUser.userId(), routineId, KoreanTime.today().minusDays(1));

        mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questDate").value(KoreanTime.today().toString()));

        String oldStatus = jdbcTemplate.queryForObject(
                "select status from user_quests where user_id = ? and quest_date = ?",
                String.class,
                testUser.userId(),
                KoreanTime.today().minusDays(1)
        );
        org.assertj.core.api.Assertions.assertThat(oldStatus).isEqualTo("expired");
    }

    private TestUser onboardedUser(String loginId) {
        User user = User.createLocalUser(loginId, "encoded-password", loginId);
        user.updateOnboardingProfile(
                "female",
                LocalDate.of(2002, 5, 20),
                BigDecimal.valueOf(164.5),
                BigDecimal.valueOf(58.2),
                "beginner",
                "none",
                "habit",
                "home",
                3,
                30,
                List.of("bodyweight")
        );
        user = userRepository.save(user);
        return new TestUser(user.getId(), jwtTokenService.issueTokenPair(user).accessToken());
    }

    private Long seedRoutineWithSession(String dayOfWeek) {
        Long routineId = insertAndReturnId("""
                insert into routines (
                    user_id,
                    name,
                    description,
                    is_default,
                    difficulty,
                    estimated_minutes,
                    is_active,
                    created_at,
                    updated_at
                )
                values (
                    null,
                    '퀘스트 테스트 루틴',
                    '퀘스트 테스트용 루틴입니다.',
                    true,
                    'easy',
                    30,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """);
        Long sessionId = insertAndReturnId("""
                insert into routine_sessions (
                    routine_id,
                    day_of_week,
                    session_name,
                    session_type,
                    seq,
                    estimated_minutes,
                    is_active,
                    created_at,
                    updated_at
                )
                values (?, ?, '오늘 세션', 'full_body', 1, 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, routineId, dayOfWeek);

        for (int i = 1; i <= 3; i++) {
            Long exerciseId = seedExercise("Quest Exercise " + i, "quest_exercise_" + i + "_" + System.nanoTime());
            jdbcTemplate.update("""
                    insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                    values (?, ?, ?, ?, 10, 3, null, 60)
                    """, routineId, sessionId, exerciseId, i);
        }
        return routineId;
    }

    private Long seedExercise(String name, String externalId) {
        return insertAndReturnId("""
                insert into exercises (
                    name,
                    category,
                    intensity,
                    external_id,
                    level,
                    equipment,
                    primary_muscles,
                    secondary_muscles,
                    instructions,
                    image_urls,
                    source,
                    source_license,
                    source_url,
                    raw_data,
                    created_at,
                    updated_at
                )
                values (
                    ?,
                    'strength',
                    'beginner',
                    ?,
                    'beginner',
                    'body only',
                    JSON '["quadriceps"]',
                    JSON '[]',
                    JSON '["test instruction"]',
                    JSON '[]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, name, externalId);
    }

    private void seedCondition(Long userId, LocalDate logDate, BigDecimal conditionScore, BigDecimal multiplier) {
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
                values (?, ?, 4, 3, 2, 2, 4, ?, ?, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, logDate, conditionScore, multiplier);
    }

    private void seedOldQuest(Long userId, Long routineId, LocalDate questDate) {
        jdbcTemplate.update("""
                insert into user_quests (
                    user_id,
                    routine_id,
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
                    proof_json,
                    quest_context_json,
                    created_at
                )
                values (?, ?, ?, 'routine', 'exercises', '어제 퀘스트', '어제 미완료 퀘스트입니다.', 1, 0, 'issued', false, 0, 0, JSON '{}', JSON '{}', CURRENT_TIMESTAMP)
                """, userId, routineId, questDate);
    }

    private void assertQuestCompletionReward(Long userId, Long questId, int expectedExp, int expectedCurrency) {
        Integer expLogCount = jdbcTemplate.queryForObject("""
                select count(*)
                from user_exp_logs
                where user_id = ? and ref_type = 'user_quest' and ref_id = ?
                """, Integer.class, userId, questId);
        Integer transactionCount = jdbcTemplate.queryForObject("""
                select count(*)
                from wallet_transactions
                where user_id = ? and tx_type = 'quest_reward' and ref_type = 'user_quest' and ref_id = ?
                """, Integer.class, userId, questId);
        Integer totalExp = jdbcTemplate.queryForObject(
                "select total_exp from users where id = ?",
                Integer.class,
                userId
        );
        Integer level = jdbcTemplate.queryForObject(
                "select level from users where id = ?",
                Integer.class,
                userId
        );
        Integer balanceCurrency = jdbcTemplate.queryForObject(
                "select balance_currency from wallets where user_id = ?",
                Integer.class,
                userId
        );

        org.assertj.core.api.Assertions.assertThat(expLogCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(transactionCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(totalExp).isEqualTo(expectedExp);
        org.assertj.core.api.Assertions.assertThat(level).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(balanceCurrency).isEqualTo(expectedCurrency);
    }

    private void assertNoQuestCompletionReward(Long userId, Long questId) {
        Integer expLogCount = jdbcTemplate.queryForObject("""
                select count(*)
                from user_exp_logs
                where user_id = ? and ref_type = 'user_quest' and ref_id = ?
                """, Integer.class, userId, questId);
        Integer transactionCount = jdbcTemplate.queryForObject("""
                select count(*)
                from wallet_transactions
                where user_id = ? and tx_type = 'quest_reward' and ref_type = 'user_quest' and ref_id = ?
                """, Integer.class, userId, questId);
        Integer totalExp = jdbcTemplate.queryForObject(
                "select total_exp from users where id = ?",
                Integer.class,
                userId
        );
        Integer walletCount = jdbcTemplate.queryForObject(
                "select count(*) from wallets where user_id = ?",
                Integer.class,
                userId
        );
        Integer balanceCurrency = walletCount == null || walletCount == 0
                ? 0
                : jdbcTemplate.queryForObject("select balance_currency from wallets where user_id = ?", Integer.class, userId);

        org.assertj.core.api.Assertions.assertThat(expLogCount).isZero();
        org.assertj.core.api.Assertions.assertThat(transactionCount).isZero();
        org.assertj.core.api.Assertions.assertThat(totalExp).isZero();
        org.assertj.core.api.Assertions.assertThat(balanceCurrency).isZero();
    }

    private Long insertAndReturnId(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement;
        };
        jdbcTemplate.update(creator, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert did not return a generated id");
        }
        return key.longValue();
    }

    private record TestUser(Long userId, String accessToken) {
    }
}
