package com.capstone.backend.exercise.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExerciseControllerTest {

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
    void exerciseListReturnsGroupedCardsWithLiked() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("exerciseListUser");
        Long swimId = seedExercise("자유형", "수영", "초급", "수영장", "free_swim", "5.0");
        seedExercise("하타 요가", "요가", "입문", "매트", "hatha_yoga", "2.5");

        mockMvc.perform(put("/api/users/me/exercises/{exerciseId}/favorite", swimId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "liked": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exerciseId").value(swimId))
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("all"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.groups", hasSize(2)))
                .andExpect(jsonPath("$.data.groups[0].category").value("수영"))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].name").value("자유형"))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].estimatedKcalPerHour").value(350))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].liked").value(true))
                .andExpect(jsonPath("$.data.groups[1].category").value("요가"))
                .andExpect(jsonPath("$.data.groups[1].exercises[0].liked").value(false));
    }

    @Test
    void favoriteScopeReturnsOnlyFavoriteExercises() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("favoriteScopeUser");
        Long favoriteId = seedExercise("바디웨이트 스쿼트", "근력", "초급", "맨몸", "body_squat", "3.5");
        seedExercise("회복 요가", "요가", "초급", "매트", "recovery_yoga", "2.0");

        mockMvc.perform(put("/api/users/me/exercises/{exerciseId}/favorite", favoriteId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "liked": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("scope", "favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("favorite"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.groups[0].category").value("근력"))
                .andExpect(jsonPath("$.data.groups[0].categoryDisplayName").value("헬스"))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].id").value(favoriteId))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].liked").value(true));
    }

    @Test
    void favoriteExercisesEndpointSupportsCategoryFilter() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("favoriteEndpointUser");
        Long cardioId = seedExercise("실내 자전거", "유산소", "초급", "실내 자전거", "favorite_indoor_bike", "4.0");
        Long yogaId = seedExercise("회복 요가", "요가", "초급", "매트", "favorite_recovery_yoga", "2.0");
        seedExercise("즐겨찾기 안 한 유산소", "유산소", "초급", "트레드밀", "unliked_cardio", "4.5");

        mockMvc.perform(put("/api/users/me/exercises/{exerciseId}/favorite", cardioId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "liked": true
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/users/me/exercises/{exerciseId}/favorite", yogaId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "liked": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/exercises/favorites")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("category", "유산소"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("favorite"))
                .andExpect(jsonPath("$.data.category").value("유산소"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.groups", hasSize(1)))
                .andExpect(jsonPath("$.data.groups[0].category").value("유산소"))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].id").value(cardioId))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].liked").value(true));
    }

    @Test
    void exerciseDetailReturnsLikedAndFavoriteCanBeRemoved() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("exerciseDetailUser");
        Long exerciseId = seedExercise("하타 요가", "요가", "입문", "매트", "hatha_detail", "2.5");

        mockMvc.perform(put("/api/users/me/exercises/{exerciseId}/favorite", exerciseId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "liked": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exercises/{exerciseId}", exerciseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("하타 요가"))
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(put("/api/users/me/exercises/{exerciseId}/favorite", exerciseId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "liked": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    void exerciseCaloriesUseUserWeightWhenAvailable() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = User.createLocalUser("weightedUser", "encoded-password", "weightedUser");
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
                List.of()
        );
        user = userRepository.save(user);
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long exerciseId = seedExercise("자유형", "수영", "초급", "수영장", "weighted_free_swim", "5.0");

        mockMvc.perform(get("/api/exercises/{exerciseId}", exerciseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedKcalPerHour").value(291));
    }

    @Test
    void exerciseListRejectsInvalidScope() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("invalidScopeUser");

        mockMvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("scope", "mine"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EXERCISE_SCOPE"));
    }

    @Test
    void exerciseListSupportsScreenFilterAliases() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("filterAliasUser");
        Long exerciseId = seedExercise("바디웨이트 스쿼트", "근력", "초급", "맨몸", "filter_body_squat", "3.5");
        seedExercise("회복 요가", "요가", "초급", "매트", "filter_recovery_yoga", "2.0");

        mockMvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("category", "헬스")
                        .queryParam("level", "입문")
                        .queryParam("keyword", "스쿼트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.groups[0].category").value("근력"))
                .andExpect(jsonPath("$.data.groups[0].categoryDisplayName").value("헬스"))
                .andExpect(jsonPath("$.data.groups[0].exercises[0].id").value(exerciseId));
    }

    private String accessTokenFor(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
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
                    JSON '["전신"]',
                    JSON '[]',
                    JSON '["테스트 운동 설명"]',
                    JSON '[]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, name, category, level, externalId, level, equipment, met);
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
