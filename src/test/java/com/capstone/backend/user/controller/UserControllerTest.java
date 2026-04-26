package com.capstone.backend.user.controller;

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
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void meReturnsCurrentUserProfile() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("demoUser", "encoded-password", "Demo User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.loginId").value("demoUser"))
                .andExpect(jsonPath("$.data.nickname").value("Demo User"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes", empty()))
                .andExpect(jsonPath("$.data.pushEnabled").value(true))
                .andExpect(jsonPath("$.data.pushQuestEnabled").value(true))
                .andExpect(jsonPath("$.data.pushRoutineEnabled").value(true))
                .andExpect(jsonPath("$.data.pushCompetitionEnabled").value(true));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void updateOnboardingProfileStoresAndReturnsProfile() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("onboardingUser", "encoded-password", "Onboarding User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

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
                                  "preferredExerciseTypes": ["strength", "walking"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Onboarding profile updated"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.gender").value("female"))
                .andExpect(jsonPath("$.data.birthDate").value("2002-05-20"))
                .andExpect(jsonPath("$.data.heightCm").value(164.5))
                .andExpect(jsonPath("$.data.weightKg").value(58.2))
                .andExpect(jsonPath("$.data.experienceLevel").value("beginner"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[0]").value("strength"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[1]").value("walking"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("female"))
                .andExpect(jsonPath("$.data.birthDate").value("2002-05-20"))
                .andExpect(jsonPath("$.data.heightCm").value(164.50))
                .andExpect(jsonPath("$.data.weightKg").value(58.20))
                .andExpect(jsonPath("$.data.experienceLevel").value("beginner"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[0]").value("strength"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[1]").value("walking"));
    }

    @Test
    void updateOnboardingProfileRejectsInvalidInput() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("invalidOnboardingUser", "encoded-password", "Invalid Onboarding User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/onboarding-profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "gender": "unknown",
                                  "birthDate": "2999-01-01",
                                  "heightCm": 20,
                                  "weightKg": 10,
                                  "experienceLevel": "expert",
                                  "preferredExerciseTypes": ["boxing"]
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/users/me/onboarding-profile"));
    }

    @Test
    void updateActiveRoutineStoresAndReturnsRoutineDetail() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("activeRoutineUser", "encoded-password", "Active Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long routineId = seedRoutine();

        mockMvc.perform(put("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "routineId": %d
                                }
                                """.formatted(routineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Active routine updated"))
                .andExpect(jsonPath("$.data.id").value(routineId))
                .andExpect(jsonPath("$.data.name").value("테스트 기본 루틴"))
                .andExpect(jsonPath("$.data.items[0].exercise.name").value("Test Squat"));

        mockMvc.perform(get("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(routineId))
                .andExpect(jsonPath("$.data.items[0].seq").value(1))
                .andExpect(jsonPath("$.data.items[0].exercise.primaryMuscles[0]").value("quadriceps"));
    }

    @Test
    void getActiveRoutineReturnsNotFoundWhenUnset() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("unsetRoutineUser", "encoded-password", "Unset Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(get("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVE_ROUTINE_NOT_SET"));
    }

    @Test
    void updateActiveRoutineReturnsNotFoundForMissingRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("missingActiveRoutineUser", "encoded-password", "Missing Active Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "routineId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void updateActiveRoutineRejectsMissingRoutineId() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("invalidActiveRoutineUser", "encoded-password", "Invalid Active Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/users/me/routines/active"));
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

        jdbcTemplate.update("""
                insert into routine_items (routine_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values (?, ?, 1, 10, 2, null, 30)
                """, routineId, exerciseId);

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
