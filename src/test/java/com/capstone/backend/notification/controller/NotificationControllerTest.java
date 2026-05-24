package com.capstone.backend.notification.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.notification.service.PushNotificationMessage;
import com.capstone.backend.notification.service.PushNotificationSender;
import com.capstone.backend.notification.service.PushSendResult;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

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

    @MockitoBean
    private PushNotificationSender pushNotificationSender;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from user_push_tokens");
        jdbcTemplate.update("delete from battle_match_queue");

        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from health_daily_summaries");

        jdbcTemplate.update("delete from users");
    }

    @Test
    void registerTokenStoresCurrentUserToken() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("pushRegisterUser");

        mockMvc.perform(post("/api/push-tokens")
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "token": "fcm-token-1",
                                  "platform": "android",
                                  "deviceId": "pixel-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("푸시 토큰이 등록되었습니다."))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.platform").value("android"))
                .andExpect(jsonPath("$.data.deviceId").value("pixel-8"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void registerTokenRejectsInvalidPlatform() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("pushInvalidPlatformUser");

        mockMvc.perform(post("/api/push-tokens")
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "token": "fcm-token-2",
                                  "platform": "web"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void sendTestNotificationSendsToEnabledUserTokens() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(pushNotificationSender.send(eq(List.of("fcm-token-3")), any(PushNotificationMessage.class)))
                .thenReturn(PushSendResult.sent(1, 1, 0));
        TestUser testUser = testUser("pushSendUser");
        long tokenId = registerToken(testUser.accessToken(), "fcm-token-3");

        mockMvc.perform(post("/api/notifications/test")
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "테스트",
                                  "body": "알림 확인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.targetCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));

        verify(pushNotificationSender).send(eq(List.of("fcm-token-3")), any(PushNotificationMessage.class));
        disableToken(testUser.accessToken(), tokenId);
    }

    @Test
    void disabledTokenIsNotUsedForTestNotification() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("pushDisableUser");
        long tokenId = registerToken(testUser.accessToken(), "fcm-token-4");

        disableToken(testUser.accessToken(), tokenId);

        mockMvc.perform(post("/api/notifications/test")
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.targetCount").value(0))
                .andExpect(jsonPath("$.data.successCount").value(0));

        verify(pushNotificationSender, never()).send(anyList(), any());
    }

    private long registerToken(String accessToken, String token) throws Exception {
        String response = mockMvc.perform(post("/api/push-tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "token": "%s",
                                  "platform": "android"
                                }
                                """.formatted(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\"id\":([0-9]+).*", "$1");
        return Long.parseLong(id);
    }

    private void disableToken(String accessToken, Long tokenId) throws Exception {
        mockMvc.perform(delete("/api/push-tokens/{tokenId}", tokenId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    private TestUser testUser(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return new TestUser(user, jwtTokenService.issueTokenPair(user).accessToken());
    }

    private record TestUser(User user, String accessToken) {
    }
}
