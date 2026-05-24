package com.capstone.backend.ranking.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
class RankingControllerTest {

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
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void weeklyActivityReturnsTopThreeByDefaultWithoutProfileImage() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUsers users = seedUsers();
        String accessToken = jwtTokenService.issueTokenPair(users.suyeon()).accessToken();
        LocalDate weekStart = KoreanTime.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        seedWeeklyRankingQuests(users, weekStart);

        mockMvc.perform(get("/api/rankings/weekly-activity")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekStartDate").value(weekStart.toString()))
                .andExpect(jsonPath("$.data.weekEndDate").value(weekStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.data.metric").value("WEEKLY_EXP"))
                .andExpect(jsonPath("$.data.rankings.length()").value(3))
                .andExpect(jsonPath("$.data.rankings[0].rank").value(1))
                .andExpect(jsonPath("$.data.rankings[0].userId").value(users.hajun().getId()))
                .andExpect(jsonPath("$.data.rankings[0].nickname").value("하준"))
                .andExpect(jsonPath("$.data.rankings[0].weeklyExp").value(80))
                .andExpect(jsonPath("$.data.rankings[0].isMe").value(false))
                .andExpect(jsonPath("$.data.rankings[0].profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.rankings[1].rank").value(2))
                .andExpect(jsonPath("$.data.rankings[1].userId").value(users.suyeon().getId()))
                .andExpect(jsonPath("$.data.rankings[1].nickname").value("수연"))
                .andExpect(jsonPath("$.data.rankings[1].weeklyExp").value(50))
                .andExpect(jsonPath("$.data.rankings[1].isMe").value(true))
                .andExpect(jsonPath("$.data.rankings[2].rank").value(3))
                .andExpect(jsonPath("$.data.rankings[2].userId").value(users.minji().getId()))
                .andExpect(jsonPath("$.data.rankings[2].weeklyExp").value(30));
    }

    @Test
    void weeklyActivitySupportsSizeParameter() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUsers users = seedUsers();
        String accessToken = jwtTokenService.issueTokenPair(users.suyeon()).accessToken();
        LocalDate weekStart = KoreanTime.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        seedWeeklyRankingQuests(users, weekStart);

        mockMvc.perform(get("/api/rankings/weekly-activity")
                        .queryParam("size", "2")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rankings.length()").value(2))
                .andExpect(jsonPath("$.data.rankings[0].nickname").value("하준"))
                .andExpect(jsonPath("$.data.rankings[1].nickname").value("수연"));
    }

    @Test
    void weeklyActivityRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/rankings/weekly-activity"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/rankings/weekly-activity"));
    }

    private TestUsers seedUsers() {
        User hajun = userRepository.save(User.createLocalUser("rankHajun", "encoded-password", "하준"));
        User suyeon = userRepository.save(User.createLocalUser("rankSuyeon", "encoded-password", "수연"));
        User minji = userRepository.save(User.createLocalUser("rankMinji", "encoded-password", "민지"));
        User oldUser = userRepository.save(User.createLocalUser("rankOld", "encoded-password", "지난주"));
        return new TestUsers(hajun, suyeon, minji, oldUser);
    }

    private void seedWeeklyRankingQuests(TestUsers users, LocalDate weekStart) {
        seedCompletedQuest(users.hajun().getId(), weekStart, 40);
        seedCompletedQuest(users.hajun().getId(), weekStart.plusDays(1), 40);
        seedCompletedQuest(users.suyeon().getId(), weekStart, 50);
        seedCompletedQuest(users.minji().getId(), weekStart, 30);
        seedCompletedQuest(users.oldUser().getId(), weekStart.minusDays(1), 200);
        seedIssuedQuest(users.minji().getId(), weekStart.plusDays(1), 999);
    }

    private void seedCompletedQuest(Long userId, LocalDate questDate, int rewardExp) {
        jdbcTemplate.update("""
                insert into user_quests (
                    user_id,
                    quest_date,
                    quest_type,
                    target_metric,
                    title,
                    description,
                    target_value,
                    progress_value,
                    status,
                    condition_adjusted,
                    reward_currency,
                    reward_exp,
                    completed_at,
                    proof_json,
                    quest_context_json,
                    created_at
                )
                values (?, ?, 'off_day', 'minutes', '완료 퀘스트', '완료된 테스트 퀘스트입니다.', 10, 10, 'completed', false, 0, ?, CURRENT_TIMESTAMP, JSON '{}', JSON '{}', CURRENT_TIMESTAMP)
                """, userId, questDate, rewardExp);
    }

    private void seedIssuedQuest(Long userId, LocalDate questDate, int rewardExp) {
        jdbcTemplate.update("""
                insert into user_quests (
                    user_id,
                    quest_date,
                    quest_type,
                    target_metric,
                    title,
                    description,
                    target_value,
                    progress_value,
                    status,
                    condition_adjusted,
                    reward_currency,
                    reward_exp,
                    proof_json,
                    quest_context_json,
                    created_at
                )
                values (?, ?, 'off_day', 'minutes', '미완료 퀘스트', '미완료 테스트 퀘스트입니다.', 10, 0, 'issued', false, 0, ?, JSON '{}', JSON '{}', CURRENT_TIMESTAMP)
                """, userId, questDate, rewardExp);
    }

    private record TestUsers(User hajun, User suyeon, User minji, User oldUser) {
    }
}
