package com.capstone.backend.achievement.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AchievementBadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuestRepository userQuestRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanupBefore() {
        cleanup();
    }

    @AfterEach
    void cleanupAfter() {
        cleanup();
    }

    @Test
    void syncBadgesAwardsCompletedQuestBadges() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("badgeQuestUser");
        seedBadge("FIRST_QUEST_COMPLETE", "첫 퀘스트 완료", "퀘스트 1회 완료");
        seedBadge("VERIFIED_QUEST_COMPLETE", "검증 완료", "건강 데이터 검증 퀘스트 1회 완료");
        seedBadge("QUEST_STREAK_3", "3일 연속 달성", "퀘스트 3일 연속 완료");
        seedCompletedQuest(testUser.user(), LocalDate.of(2026, 5, 20), Map.of("battleEligible", true));
        seedCompletedQuest(testUser.user(), LocalDate.of(2026, 5, 21), Map.of());
        seedCompletedQuest(testUser.user(), LocalDate.of(2026, 5, 22), Map.of());

        mockMvc.perform(post("/api/users/me/badges/sync")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("배지 지급 상태를 동기화했습니다."))
                .andExpect(jsonPath("$.data.earnedCount").value(3))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.badges[0].badgeCode").value("FIRST_QUEST_COMPLETE"))
                .andExpect(jsonPath("$.data.badges[0].earned").value(true))
                .andExpect(jsonPath("$.data.badges[1].badgeCode").value("VERIFIED_QUEST_COMPLETE"))
                .andExpect(jsonPath("$.data.badges[1].earned").value(true))
                .andExpect(jsonPath("$.data.badges[2].badgeCode").value("QUEST_STREAK_3"))
                .andExpect(jsonPath("$.data.badges[2].earned").value(true));

        Integer ownedBadgeCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from user_items user_item
                where user_item.user_id = ?
                  and user_item.quantity = 1
                """,
                Integer.class,
                testUser.userId()
        );
        assertThat(ownedBadgeCount).isEqualTo(3);
    }

    @Test
    void shopItemsBadgeFilterSeparatesAchievementBadgesFromPasses() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("badgeFilterUser");
        seedItem("pvp_badge", "기록 방어권", 300, true, "{\"effect\":\"최고 기록 보호 · 1회\"}");
        seedBadge("FIRST_BATTLE_JOIN", "첫 배틀 참가", "배틀 1회 참가 및 결과 확정");

        mockMvc.perform(get("/api/shop/items")
                        .queryParam("type", "pass")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("기록 방어권"))
                .andExpect(jsonPath("$.data.items[0].category").value("pass"));

        mockMvc.perform(get("/api/shop/items")
                        .queryParam("type", "badge")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("첫 배틀 참가"))
                .andExpect(jsonPath("$.data.items[0].category").value("badge"))
                .andExpect(jsonPath("$.data.items[0].sellable").value(false));
    }

    private void seedCompletedQuest(User user, LocalDate questDate, Map<String, Object> proof) {
        UserQuest quest = UserQuest.create(
                user,
                null,
                null,
                null,
                questDate,
                UserQuest.TYPE_ROUTINE,
                UserQuest.METRIC_ROUTINE,
                "테스트 퀘스트",
                "테스트 퀘스트입니다.",
                1,
                false,
                Map.of()
        );
        quest.complete(1, proof);
        userQuestRepository.save(quest);
    }

    private Long seedBadge(String badgeCode, String name, String criteria) {
        String metadata = """
                {"kind":"achievement_badge","badgeCode":"%s","criteria":"%s"}
                """.formatted(badgeCode, criteria);
        return seedItem("pvp_badge", name, 0, false, metadata);
    }

    private Long seedItem(String itemType, String name, int priceCurrency, boolean sellable, String metadataJson) {
        String metadataLiteral = metadataJson.replace("'", "''");
        return insertAndReturnId("""
                insert into items (
                    item_type,
                    name,
                    description,
                    price_currency,
                    is_sellable,
                    image_url,
                    metadata,
                    is_active,
                    created_at
                )
                values (?, ?, '테스트 아이템입니다.', ?, ?, '/media/assets/test_item.png', JSON '%s', true, CURRENT_TIMESTAMP)
                """.formatted(metadataLiteral), itemType, name, priceCurrency, sellable);
    }

    private TestUser testUser(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return new TestUser(user, jwtTokenService.issueTokenPair(user).accessToken());
    }

    private void cleanup() {
        jdbcTemplate.update("delete from battle_match_queue");
        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from user_item_effects");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from user_items");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from items");
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
        return keyHolder.getKey().longValue();
    }

    private record TestUser(User user, String accessToken) {
        Long userId() {
            return user.getId();
        }
    }
}
