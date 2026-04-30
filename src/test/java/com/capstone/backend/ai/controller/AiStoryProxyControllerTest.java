package com.capstone.backend.ai.controller;

import com.capstone.backend.ai.service.AiProxyService;
import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiStoryProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AiProxyService aiProxyService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void healthProxiesAiRootEndpoint() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        Map<String, Object> response = Map.of("message", "story generation server is running");
        when(aiProxyService.get("/")).thenReturn(response);

        mockMvc.perform(get("/api/ai/health")
                        .header("Authorization", "Bearer " + accessTokenFor("aiHealthUser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI 서버 상태 확인에 성공했습니다."))
                .andExpect(jsonPath("$.data.message").value("story generation server is running"));

        verify(aiProxyService).get("/");
    }

    @Test
    void playStoryProxiesRequestAndReturnsAiResponseAsIs() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        Map<String, Object> response = Map.of(
                "scenario_id", 4,
                "chapter_num", 1,
                "phase", "INTRO",
                "unit_index", 0,
                "total_units", 12,
                "is_chapter_completed", false,
                "is_story_completed", false
        );
        when(aiProxyService.post(eq("/stories/play"), any())).thenReturn(response);

        mockMvc.perform(post("/api/stories/play")
                        .header("Authorization", "Bearer " + accessTokenFor("aiPlayUser"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "user_id": 100,
                                  "scenario_id": 4,
                                  "user_message": null,
                                  "choice_id": null,
                                  "restart": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI 스토리 진행 응답을 조회했습니다."))
                .andExpect(jsonPath("$.data.scenario_id").value(4))
                .andExpect(jsonPath("$.data.chapter_num").value(1))
                .andExpect(jsonPath("$.data.phase").value("INTRO"))
                .andExpect(jsonPath("$.data.is_story_completed").value(false));

        verify(aiProxyService).post(eq("/stories/play"), any());
    }

    @Test
    void playStoryConvertsAiErrorToProblemDetail() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(aiProxyService.post(eq("/stories/play"), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "AI_SERVER_ERROR", "scenario_id=999999 시나리오를 찾을 수 없습니다."));

        mockMvc.perform(post("/api/stories/play")
                        .header("Authorization", "Bearer " + accessTokenFor("aiPlayErrorUser"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "user_id": 100,
                                  "scenario_id": 999999,
                                  "restart": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AI_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value("scenario_id=999999 시나리오를 찾을 수 없습니다."));
    }

    @Test
    void storyProxyRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/stories/play")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/stories/play"));
    }

    private String accessTokenFor(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }
}
