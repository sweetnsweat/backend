package com.capstone.backend.condition.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConditionControllerTest {

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
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void updateTodayConditionCreatesAndReturnsCalculatedValues() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("conditionUser");

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
                .andExpect(jsonPath("$.message").value("오늘 컨디션이 저장되었습니다."))
                .andExpect(jsonPath("$.data.conditionLevel").value(4))
                .andExpect(jsonPath("$.data.sleepScore").value(3))
                .andExpect(jsonPath("$.data.stressScore").value(2))
                .andExpect(jsonPath("$.data.energyLevel").value(4))
                .andExpect(jsonPath("$.data.conditionScore").value(78.75))
                .andExpect(jsonPath("$.data.exerciseMultiplier").value(1.00));
    }

    @Test
    void getTodayConditionReturnsExistingCondition() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("conditionLookupUser");

        mockMvc.perform(put("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "conditionLevel": 5,
                                  "sleepScore": 4,
                                  "stressScore": 1,
                                  "energyLevel": 5
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionLevel").value(5))
                .andExpect(jsonPath("$.data.sleepScore").value(4))
                .andExpect(jsonPath("$.data.stressScore").value(1))
                .andExpect(jsonPath("$.data.energyLevel").value(5))
                .andExpect(jsonPath("$.data.conditionScore").value(100.00))
                .andExpect(jsonPath("$.data.exerciseMultiplier").value(1.15));
    }

    @Test
    void updateTodayConditionUpdatesExistingConditionForSameDate() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("conditionUpdateUser");

        mockMvc.perform(put("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "conditionLevel": 5,
                                  "sleepScore": 4,
                                  "stressScore": 1,
                                  "energyLevel": 5
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "conditionLevel": 1,
                                  "sleepScore": 1,
                                  "stressScore": 5,
                                  "energyLevel": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionLevel").value(1))
                .andExpect(jsonPath("$.data.sleepScore").value(1))
                .andExpect(jsonPath("$.data.stressScore").value(5))
                .andExpect(jsonPath("$.data.energyLevel").value(1))
                .andExpect(jsonPath("$.data.conditionScore").value(21.25))
                .andExpect(jsonPath("$.data.exerciseMultiplier").value(0.70));

        mockMvc.perform(get("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionScore").value(21.25));
    }

    @Test
    void getTodayConditionReturnsNotFoundWhenMissing() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("conditionMissingUser");

        mockMvc.perform(get("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONDITION_NOT_FOUND"));
    }

    @Test
    void updateTodayConditionRejectsInvalidInput() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("conditionInvalidUser");

        mockMvc.perform(put("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "sleepScore": 0,
                                  "stressScore": 6
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.instance").value("/api/conditions/today"));
    }

    private String accessTokenFor(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }
}
