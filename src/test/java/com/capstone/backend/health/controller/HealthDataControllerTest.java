package com.capstone.backend.health.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthDataControllerTest {

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
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from health_daily_summaries");

        jdbcTemplate.update("delete from users");
    }

    @Test
    void syncNormalizesHealthConnectSamples() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("healthConnectUser");

        mockMvc.perform(post("/api/health-data/sync")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "samples": [
                                    {
                                      "source": "health_connect",
                                      "rawRecordType": "Steps",
                                      "value": 4815,
                                      "startTime": "2026-05-14T15:00:00Z",
                                      "endTime": "2026-05-15T14:59:59.999Z",
                                      "dataOrigin": "com.sec.android.app.shealth"
                                    },
                                    {
                                      "source": "health_connect",
                                      "rawRecordType": "HeartRate",
                                      "value": 87,
                                      "startTime": "2026-05-15T08:20:16Z",
                                      "dataOrigin": "com.sec.android.app.shealth"
                                    },
                                    {
                                      "source": "health_connect",
                                      "rawRecordType": "HeartRate",
                                      "value": 99,
                                      "startTime": "2026-05-15T08:30:10Z",
                                      "dataOrigin": "com.sec.android.app.shealth"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptedSamples").value(3))
                .andExpect(jsonPath("$.data.countByType.STEPS").value(1))
                .andExpect(jsonPath("$.data.countByType.HEART_RATE").value(2))
                .andExpect(jsonPath("$.data.summaries[0].type").value("HEART_RATE"))
                .andExpect(jsonPath("$.data.summaries[0].average").value(93.00))
                .andExpect(jsonPath("$.data.summaries[0].max").value(99))
                .andExpect(jsonPath("$.data.summaries[1].type").value("STEPS"))
                .andExpect(jsonPath("$.data.summaries[1].total").value(4815));

        Long userId = jdbcTemplate.queryForObject("select id from users where login_id = 'healthConnectUser'", Long.class);
        Integer steps = jdbcTemplate.queryForObject("""
                select steps
                from health_daily_summaries
                where user_id = ?
                  and summary_date = date '2026-05-15'
                """, Integer.class, userId);
        org.assertj.core.api.Assertions.assertThat(steps).isEqualTo(4815);
    }

    @Test
    void syncNormalizesHealthKitSamplesToSameMetricNames() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("healthKitUser");

        mockMvc.perform(post("/api/health-data/sync")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "samples": [
                                    {
                                      "source": "healthkit",
                                      "rawRecordType": "HKQuantityTypeIdentifierStepCount",
                                      "value": 3200,
                                      "startTime": "2026-05-15T00:00:00Z",
                                      "endTime": "2026-05-15T23:59:59Z",
                                      "dataOrigin": "com.apple.Health"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countByType.STEPS").value(1))
                .andExpect(jsonPath("$.data.summaries[0].type").value("STEPS"))
                .andExpect(jsonPath("$.data.summaries[0].total").value(3200));

        Long userId = jdbcTemplate.queryForObject("select id from users where login_id = 'healthKitUser'", Long.class);
        Integer steps = jdbcTemplate.queryForObject("""
                select steps
                from health_daily_summaries
                where user_id = ?
                  and summary_date = date '2026-05-15'
                """, Integer.class, userId);
        org.assertj.core.api.Assertions.assertThat(steps).isEqualTo(3200);
    }

    private String accessTokenFor(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }
}
