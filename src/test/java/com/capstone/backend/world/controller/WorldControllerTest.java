package com.capstone.backend.world.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorldControllerTest {

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
        jdbcTemplate.update("delete from story_progress");
        jdbcTemplate.update("delete from character_profiles");
        jdbcTemplate.update("delete from scenarios");
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
    }

    @Test
    void rankingsReturnsInProgressScenarioScoresOnly() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessToken("worldRankingUser");
        seedScenarios();
        seedCharacterProfiles();
        seedStoryProgress();

        mockMvc.perform(get("/api/worlds/rankings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metric").value("ACTIVE_CHAT_COUNT"))
                .andExpect(jsonPath("$.data.rankings.length()").value(3))
                .andExpect(jsonPath("$.data.rankings[0].rank").value(1))
                .andExpect(jsonPath("$.data.rankings[0].scenarioId").value(2))
                .andExpect(jsonPath("$.data.rankings[0].worldTitle").value("하륜의 세계"))
                .andExpect(jsonPath("$.data.rankings[0].displayName").value("하륜"))
                .andExpect(jsonPath("$.data.rankings[0].imageUrl").value("/media/assets/character_haryun.png"))
                .andExpect(jsonPath("$.data.rankings[0].score").value(3))
                .andExpect(jsonPath("$.data.rankings[0].activeChatCount").doesNotExist())
                .andExpect(jsonPath("$.data.rankings[1].rank").value(2))
                .andExpect(jsonPath("$.data.rankings[1].scenarioId").value(1))
                .andExpect(jsonPath("$.data.rankings[1].score").value(2))
                .andExpect(jsonPath("$.data.rankings[2].rank").value(3))
                .andExpect(jsonPath("$.data.rankings[2].scenarioId").value(3))
                .andExpect(jsonPath("$.data.rankings[2].score").value(1));
    }

    @Test
    void rankingsSupportsLimitParameter() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessToken("worldRankingLimitUser");
        seedScenarios();
        seedCharacterProfiles();
        seedStoryProgress();

        mockMvc.perform(get("/api/worlds/rankings")
                        .queryParam("limit", "2")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rankings.length()").value(2))
                .andExpect(jsonPath("$.data.rankings[0].scenarioId").value(2))
                .andExpect(jsonPath("$.data.rankings[1].scenarioId").value(1));
    }

    @Test
    void rankingsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/rankings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/worlds/rankings"));
    }

    private String accessToken(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }

    private void seedScenarios() {
        jdbcTemplate.update("""
                insert into scenarios (id, title, summary, genre, thumbnail_url, world_image_url, is_active)
                values
                    (1, '카이렌의 세계', '요약 1', '로맨스 판타지', '/media/assets/thumb_1.png', '/media/assets/world_1.png', true),
                    (2, '하륜의 세계', '요약 2', '무협', '/media/assets/thumb_2.png', '/media/assets/world_2.png', true),
                    (3, '서도윤의 세계', '요약 3', '현대 드라마', '/media/assets/thumb_3.png', '/media/assets/world_3.png', true),
                    (4, '비활성 세계관', '요약 4', '테스트', '/media/assets/thumb_4.png', '/media/assets/world_4.png', false)
                """);
    }

    private void seedCharacterProfiles() {
        jdbcTemplate.update("""
                insert into character_profiles (id, scenario_id, name, character_title, character_type, image_url, mid_story_line, is_representative)
                values
                    (1, 1, '카이렌', '황태자', 'main', '/media/assets/character_kairen.png', '대사 1', true),
                    (2, 2, '하륜', '라이벌', 'main', '/media/assets/character_haryun.png', '대사 2', true),
                    (3, 3, '서도윤', '선배', 'main', '/media/assets/character_doyun.png', '대사 3', true),
                    (4, 4, '비활성', '테스트', 'main', '/media/assets/character_inactive.png', '대사 4', true)
                """);
    }

    private void seedStoryProgress() {
        jdbcTemplate.update("""
                insert into story_progress (id, user_key, scenario_id, status, created_at, updated_at)
                values
                    (1, '11', 1, 'IN_PROGRESS', now(), now()),
                    (2, '12', 1, 'IN_PROGRESS', now(), now()),
                    (3, '11', 1, 'COMPLETED', now(), now()),
                    (4, '21', 2, 'IN_PROGRESS', now(), now()),
                    (5, '22', 2, 'IN_PROGRESS', now(), now()),
                    (6, '23', 2, 'IN_PROGRESS', now(), now()),
                    (7, '24', 2, 'STORY_COMPLETED', now(), now()),
                    (8, '31', 3, 'IN_PROGRESS', now(), now()),
                    (9, '41', 4, 'IN_PROGRESS', now(), now())
                """);
    }
}
