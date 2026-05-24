package com.capstone.backend.home.controller;

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
class HomeControllerTest {

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
        jdbcTemplate.update("delete from scenario_genres");
        jdbcTemplate.update("delete from character_profiles");
        jdbcTemplate.update("delete from scenarios");
        jdbcTemplate.update("delete from battle_match_queue");

        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
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
        jdbcTemplate.update("delete from health_daily_summaries");

        jdbcTemplate.update("delete from users");
    }

    @Test
    void worldBannersReturnsDefaultThreeActiveSlides() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessToken("homeBannerUser");
        seedScenarios();
        seedCharacterProfiles();

        mockMvc.perform(get("/api/home/world-banners")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slides.length()").value(3))
                .andExpect(jsonPath("$.data.slides[0].scenarioId").value(4))
                .andExpect(jsonPath("$.data.slides[0].worldTitle").value("월하검귀는 다시 웃지 않는다"))
                .andExpect(jsonPath("$.data.slides[0].genre").value("무협 회귀 복수 로맨스"))
                .andExpect(jsonPath("$.data.slides[0].summary").value("복수와 회귀를 다루는 무협 세계관"))
                .andExpect(jsonPath("$.data.slides[0].imageUrl").value("http://localhost:8000/media/assets/character_cheon.png"))
                .andExpect(jsonPath("$.data.slides[0].backgroundImageUrl").value("http://localhost:8000/media/assets/world_4.png"))
                .andExpect(jsonPath("$.data.slides[0].representativeCharacterName").value("천류하"))
                .andExpect(jsonPath("$.data.slides[0].representativeCharacterTitle").value("검귀"))
                .andExpect(jsonPath("$.data.slides[0].headline").value("천류하"))
                .andExpect(jsonPath("$.data.slides[0].quote").value("네 뒤를 쫓아가도, 결코 네 그림자를 벗어날 수 없다는 걸 알게 될 거야."))
                .andExpect(jsonPath("$.data.slides[1].scenarioId").value(3))
                .andExpect(jsonPath("$.data.slides[2].scenarioId").value(2));
    }

    @Test
    void worldBannersSupportsLimitParameter() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessToken("homeBannerLimitUser");
        seedScenarios();
        seedCharacterProfiles();

        mockMvc.perform(get("/api/home/world-banners")
                        .queryParam("limit", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slides.length()").value(1))
                .andExpect(jsonPath("$.data.slides[0].scenarioId").value(4));
    }

    @Test
    void worldBannersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/home/world-banners"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/home/world-banners"));
    }

    private String accessToken(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }

    private void seedScenarios() {
        jdbcTemplate.update("""
                insert into scenarios (id, title, summary, genre, thumbnail_url, world_image_url, is_active)
                values
                    (1, '제목 미정 1', '요약 1', '로맨스 판타지', '/media/assets/thumb_1.png', '/media/assets/world_1.png', true),
                    (2, '제목 미정 2', '요약 2', '현대 드라마', '/media/assets/thumb_2.png', '/media/assets/world_2.png', true),
                    (3, '두 번째 결혼식의 공범', '요약 3', '현대 막장 회귀 복수 드라마', '/media/assets/thumb_3.png', '/media/assets/world_3.png', true),
                    (4, '월하검귀는 다시 웃지 않는다', '복수와 회귀를 다루는 무협 세계관', '무협 회귀 복수 로맨스', '/media/assets/thumb_4.png', '/media/assets/world_4.png', true),
                    (5, '비활성 세계관', '노출되면 안 됨', '테스트', '/media/assets/thumb_5.png', '/media/assets/world_5.png', false)
                """);
    }

    private void seedCharacterProfiles() {
        jdbcTemplate.update("""
                insert into character_profiles (id, scenario_id, name, character_title, character_type, image_url, mid_story_line, is_representative)
                values
                    (1, 1, '카이렌', '황태자', 'main', '/media/assets/character_1.png', '대사 1', true),
                    (2, 2, '하륜', '라이벌', 'main', '/media/assets/character_2.png', '대사 2', true),
                    (3, 3, '서도윤', '선배', 'main', '/media/assets/character_3.png', '대사 3', true),
                    (4, 4, '월령한', '조력자', 'sub', '/media/assets/character_sub.png', '서브 대사', false),
                    (5, 4, '천류하', '검귀', 'main', '/media/assets/character_cheon.png', '네 뒤를 쫓아가도, 결코 네 그림자를 벗어날 수 없다는 걸 알게 될 거야.', true)
                """);
    }
}
