package com.capstone.backend.routine.controller;

import com.capstone.backend.auth.security.JwtTokenService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoutineControllerTest {

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
        jdbcTemplate.update("delete from user_quests");
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
    void defaultRoutinesReturnsActiveDefaultRoutineSummaries() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("routineUser");
        Long routineId = seedRoutine();

        mockMvc.perform(get("/api/routines/default")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(routineId))
                .andExpect(jsonPath("$.data[0].name").value("테스트 기본 루틴"))
                .andExpect(jsonPath("$.data[0].difficulty").value("easy"))
                .andExpect(jsonPath("$.data[0].estimatedMinutes").value(15))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    void recommendationsReturnBestMatchingDefaultRoutines() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = User.createLocalUser("recommendationUser", "encoded-password", "Recommendation User");
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
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long homeRoutineId = seedRecommendedRoutine(
                "초급 홈트 루틴",
                "beginner",
                "JSON '[\"none\", \"occasional\"]'",
                "JSON '[\"habit\", \"stamina\"]'",
                "JSON '[\"home\"]'",
                3,
                25,
                "JSON '[\"bodyweight\"]'"
        );
        seedRecommendedRoutine(
                "초급 헬스장 루틴",
                "beginner",
                "JSON '[\"none\", \"occasional\"]'",
                "JSON '[\"strength\"]'",
                "JSON '[\"gym\"]'",
                3,
                30,
                "JSON '[\"strength\"]'"
        );

        mockMvc.perform(get("/api/routines/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].routine.id").value(homeRoutineId))
                .andExpect(jsonPath("$.data[0].routine.name").value("초급 홈트 루틴"))
                .andExpect(jsonPath("$.data[0].score").value(115))
                .andExpect(jsonPath("$.data[0].reasons[0]").exists())
                .andExpect(jsonPath("$.data[0].routine.placeTypes[0]").value("home"))
                .andExpect(jsonPath("$.data[1].routine.name").value("초급 헬스장 루틴"));
    }

    @Test
    void recommendationsRequireOnboardingProfile() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("recommendationOnboardingRequiredUser");
        seedRecommendedRoutine(
                "초급 홈트 루틴",
                "beginner",
                "JSON '[\"none\"]'",
                "JSON '[\"habit\"]'",
                "JSON '[\"home\"]'",
                3,
                25,
                "JSON '[\"bodyweight\"]'"
        );

        mockMvc.perform(get("/api/routines/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void routineDetailReturnsRoutineItemsAndExerciseDetails() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("routineDetailUser");
        Long routineId = seedRoutine();

        mockMvc.perform(get("/api/routines/{routineId}", routineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(routineId))
                .andExpect(jsonPath("$.data.name").value("테스트 기본 루틴"))
                .andExpect(jsonPath("$.data.items[0].seq").value(1))
                .andExpect(jsonPath("$.data.items[0].reps").value(10))
                .andExpect(jsonPath("$.data.items[0].sets").value(2))
                .andExpect(jsonPath("$.data.items[0].restSec").value(30))
                .andExpect(jsonPath("$.data.items[0].exercise.name").value("Test Squat"))
                .andExpect(jsonPath("$.data.items[0].exercise.category").value("strength"))
                .andExpect(jsonPath("$.data.items[0].exercise.primaryMuscles[0]").value("quadriceps"))
                .andExpect(jsonPath("$.data.items[0].exercise.imageUrls[0]").value("https://example.com/test-squat.jpg"))
                .andExpect(jsonPath("$.data.sessions[0].seq").value(1))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeekDisplayName").value("월요일"))
                .andExpect(jsonPath("$.data.sessions[0].sessionName").value("전신 A"))
                .andExpect(jsonPath("$.data.sessions[0].sessionType").value("full_body"))
                .andExpect(jsonPath("$.data.sessions[0].sessionTypeDisplayName").value("전신"))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.name").value("Test Squat"));
    }

    @Test
    void routineDetailReturnsNotFoundForMissingRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("missingRoutineUser");

        mockMvc.perform(get("/api/routines/{routineId}", 999999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void activateDefaultRoutineCopiesOnceAndSetsUserActiveRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("activateRoutineUser", "encoded-password", "Activate Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long defaultRoutineId = seedRoutine();

        mockMvc.perform(post("/api/routines/{routineId}/activate", defaultRoutineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.sourceRoutineId").value(defaultRoutineId))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.name").value("Test Squat"));

        mockMvc.perform(post("/api/routines/{routineId}/activate", defaultRoutineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceRoutineId").value(defaultRoutineId));

        Integer copiedRoutineCount = jdbcTemplate.queryForObject(
                "select count(*) from routines where user_id = ? and source_routine_id = ?",
                Integer.class,
                user.getId(),
                defaultRoutineId
        );
        org.assertj.core.api.Assertions.assertThat(copiedRoutineCount).isEqualTo(1);

        Long activeRoutineSourceId = jdbcTemplate.queryForObject("""
                select r.source_routine_id
                from users u
                join routines r on r.id = u.active_routine_id
                where u.id = ?
                """, Long.class, user.getId());
        org.assertj.core.api.Assertions.assertThat(activeRoutineSourceId).isEqualTo(defaultRoutineId);
    }

    private String accessTokenFor(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }

    private Long seedRoutine() {
        Long exerciseId = insertAndReturnId("""
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
                    'Test Squat',
                    'strength',
                    'beginner',
                    ?,
                    'beginner',
                    'body only',
                    JSON '["quadriceps"]',
                    JSON '["glutes"]',
                    JSON '["Stand with feet shoulder-width apart.", "Bend knees and lower hips."]',
                    JSON '["https://example.com/test-squat.jpg"]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, "test_squat_" + System.nanoTime());

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
                    '테스트 기본 루틴',
                    '테스트용 기본 루틴입니다.',
                    true,
                    'easy',
                    15,
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
                values (
                    ?,
                    'MONDAY',
                    '전신 A',
                    'full_body',
                    1,
                    15,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, routineId);

        jdbcTemplate.update("""
                insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values (?, ?, ?, 1, 10, 2, null, 30)
                """, routineId, sessionId, exerciseId);

        return routineId;
    }

    private Long seedRecommendedRoutine(String name,
                                        String targetExperienceLevel,
                                        String targetCurrentExerciseStatuses,
                                        String goalTypes,
                                        String placeTypes,
                                        Integer weeklyFrequency,
                                        Integer estimatedMinutes,
                                        String recommendedExerciseTypes) {
        return insertAndReturnId("""
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
                    ?,
                    '추천 테스트 루틴입니다.',
                    true,
                    'easy',
                    ?,
                    ?,
                    %s,
                    %s,
                    %s,
                    ?,
                    %s,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """.formatted(targetCurrentExerciseStatuses, goalTypes, placeTypes, recommendedExerciseTypes),
                name, estimatedMinutes, targetExperienceLevel, weeklyFrequency);
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
