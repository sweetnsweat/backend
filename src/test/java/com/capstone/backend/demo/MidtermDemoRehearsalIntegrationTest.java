package com.capstone.backend.demo;

import com.capstone.backend.global.time.KoreanTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MidtermDemoRehearsalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void cleanup() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        jdbcTemplate.update("delete from battle_match_queue");


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
        jdbcTemplate.update("delete from health_daily_summaries");

        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void midtermDemoRehearsalFlowPassesUntilDailyQuestDeduplication() throws Exception {
        Long chestPressId = seedExercise("Chest Press", "근력", "초급", "머신", "rehearsal_chest_press", "4.0");
        Long squatId = seedExercise("Bodyweight Squat", "근력", "초급", "맨몸", "rehearsal_squat", "3.5");
        Long yogaId = seedExercise("Recovery Yoga", "요가", "입문", "매트", "rehearsal_yoga", "2.0");
        Long defaultRoutineId = seedDefaultRoutine(chestPressId, squatId, yogaId);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {
                                  "loginId": "rehearsalUser",
                                  "password": "password123",
                                  "nickname": "리허설사용자"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.loginId").value("rehearsalUser"));

        String accessToken = loginAndReturnAccessToken();

        mockMvc.perform(put("/api/users/me/onboarding-profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "gender": "female",
                                  "birthDate": "2002-05-20",
                                  "heightCm": 164.5,
                                  "weightKg": 58.2,
                                  "experienceLevel": "beginner",
                                  "currentExerciseStatus": "none",
                                  "fitnessGoal": "habit",
                                  "preferredWorkoutPlace": "home",
                                  "weeklyWorkoutFrequency": 3,
                                  "availableWorkoutMinutes": 30,
                                  "preferredExerciseTypes": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.todayConditionCompleted").value(true))
                .andExpect(jsonPath("$.data.routineSetupRequired").value(true))
                .andExpect(jsonPath("$.data.preferredExerciseTypes").isEmpty());

        mockMvc.perform(get("/api/exercises/categories")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.categories.length()", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("category", "헬스")
                        .queryParam("level", "입문")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.groups[0].categoryDisplayName").value("헬스"));

        mockMvc.perform(get("/api/exercises/{exerciseId}", chestPressId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(chestPressId))
                .andExpect(jsonPath("$.data.met").value(4.0))
                .andExpect(jsonPath("$.data.level").value("초급"))
                .andExpect(jsonPath("$.data.equipment").value("머신"))
                .andExpect(jsonPath("$.data.primaryMuscles[0]").value("가슴"))
                .andExpect(jsonPath("$.data.instructions[0]").value("테스트 운동 설명"));

        mockMvc.perform(get("/api/routines/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].routine.id").value(defaultRoutineId))
                .andExpect(jsonPath("$.data[0].reasons[0]").exists());

        JsonNode activatedRoutine = json(mockMvc.perform(post("/api/routines/{routineId}/activate", defaultRoutineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.sourceRoutineId").value(defaultRoutineId))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.name").value("Chest Press"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        Long activeRoutineId = activatedRoutine.at("/data/id").asLong();

        mockMvc.perform(get("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(activeRoutineId))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeek").value(KoreanTime.today().getDayOfWeek().name()));

        mockMvc.perform(put("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "conditionLevel": 4,
                                  "sleepScore": 3,
                                  "stressScore": 2,
                                  "energyLevel": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionScore").value(72.92))
                .andExpect(jsonPath("$.data.exerciseMultiplier").value(1.0));

        JsonNode firstQuest = json(mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questType").value("ROUTINE"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.routineId").value(activeRoutineId))
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Chest Press"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        JsonNode secondQuest = json(mockMvc.perform(get("/api/quests/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(secondQuest.at("/data/id").asLong()).isEqualTo(firstQuest.at("/data/id").asLong());

        Integer questCount = jdbcTemplate.queryForObject(
                "select count(*) from user_quests where quest_date = ?",
                Integer.class,
                KoreanTime.today()
        );
        assertThat(questCount).isEqualTo(1);
    }

    private String loginAndReturnAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "loginId": "rehearsalUser",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json(response).at("/data/accessToken").asText();
    }

    private JsonNode json(String content) throws Exception {
        return objectMapper.readTree(content);
    }

    private Long seedExercise(String name, String category, String level, String equipment, String externalId, String met) {
        return insertAndReturnId("""
                insert into exercises (
                    name,
                    category,
                    intensity,
                    external_id,
                    level,
                    equipment,
                    met,
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
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    JSON '["가슴"]',
                    JSON '["삼두"]',
                    JSON '["테스트 운동 설명"]',
                    JSON '["https://example.com/exercise.jpg"]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, name, category, level, externalId, level, equipment, met);
    }

    private Long seedDefaultRoutine(Long firstExerciseId, Long secondExerciseId, Long thirdExerciseId) {
        Long routineId = insertAndReturnId("""
                insert into routines (
                    user_id,
                    name,
                    description,
                    is_default,
                    difficulty,
                    estimated_minutes,
                    target_experience_level,
                    target_current_exercise_statuses,
                    goal_types,
                    place_types,
                    weekly_frequency,
                    recommended_exercise_types,
                    is_active,
                    created_at,
                    updated_at
                )
                values (
                    null,
                    '리허설 초급 전신 루틴',
                    '발표 리허설용 기본 루틴입니다.',
                    true,
                    'easy',
                    30,
                    'beginner',
                    JSON '["none", "occasional"]',
                    JSON '["habit", "strength"]',
                    JSON '["home", "gym"]',
                    3,
                    JSON '["bodyweight", "strength"]',
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
                values (?, ?, '오늘 전신 세션', 'full_body', 1, 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, routineId, KoreanTime.today().getDayOfWeek().name());

        jdbcTemplate.update("""
                insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values
                    (?, ?, ?, 1, 10, 3, null, 60),
                    (?, ?, ?, 2, 12, 2, null, 45),
                    (?, ?, ?, 3, null, null, 600, 30)
                """, routineId, sessionId, firstExerciseId, routineId, sessionId, secondExerciseId, routineId, sessionId, thirdExerciseId);

        return routineId;
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
}
