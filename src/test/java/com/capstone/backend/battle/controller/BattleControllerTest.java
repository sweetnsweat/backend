package com.capstone.backend.battle.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.battle.entity.Battle;
import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.notification.service.NotificationService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.List;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BattleControllerTest {

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
    private NotificationService notificationService;

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

    @Test
    void summaryReturnsDefaultBattleStats() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser user = testUser("battleSummaryUser", "요약유저");

        mockMvc.perform(get("/api/battles/me/summary")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rankName").value("Unranked"))
                .andExpect(jsonPath("$.data.wins").value(0))
                .andExpect(jsonPath("$.data.losses").value(0))
                .andExpect(jsonPath("$.data.draws").value(0))
                .andExpect(jsonPath("$.data.winRate").value(0))
                .andExpect(jsonPath("$.data.currentDailyBattle").doesNotExist())
                .andExpect(jsonPath("$.data.currentWeeklyBattle").doesNotExist());
    }

    @Test
    void matchQueuesFirstUserThenCreatesBattleWhenSecondUserJoinsAndReusesCurrentBattle() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleMe", "나");
        TestUser opponent = testUser("battleOpponent", "상대");
        LocalDate today = KoreanTime.today();
        seedCompletedQuest(me.userId(), today, "routine", healthProof(30, 4000, 3000, 200));
        seedCompletedQuest(opponent.userId(), today, "routine", healthProof(25, 2500, 1800, 150));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + opponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Battle queued"))
                .andExpect(jsonPath("$.data.mode").value("DAILY"))
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.queuedAt").exists())
                .andExpect(jsonPath("$.data.participants.length()").value(1))
                .andExpect(jsonPath("$.data.participants[0].userId").value(opponent.userId()))
                .andExpect(jsonPath("$.data.participants[0].score").value(0));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + opponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        Integer queuedBattleCount = jdbcTemplate.queryForObject("select count(*) from battles", Integer.class);
        Integer waitingQueueCount = jdbcTemplate.queryForObject("""
                select count(*)
                from battle_match_queue
                where status = 'WAITING'
                """, Integer.class);
        org.assertj.core.api.Assertions.assertThat(queuedBattleCount).isZero();
        org.assertj.core.api.Assertions.assertThat(waitingQueueCount).isEqualTo(1);
        verify(notificationService, never()).sendBattleMatched(any(Battle.class), anyList());

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Battle matched"))
                .andExpect(jsonPath("$.data.mode").value("DAILY"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.matchStatus").value("MATCHED"))
                .andExpect(jsonPath("$.data.participants.length()").value(2))
                .andExpect(jsonPath("$.data.participants[0].userId").value(me.userId()))
                .andExpect(jsonPath("$.data.participants[0].score").value(0))
                .andExpect(jsonPath("$.data.participants[1].userId").value(opponent.userId()))
                .andExpect(jsonPath("$.data.participants[1].score").value(0))
                .andExpect(jsonPath("$.data.score.leadingUserId").doesNotExist())
                .andExpect(jsonPath("$.data.metrics[0].metricKey").value("TOTAL_SCORE"))
                .andExpect(jsonPath("$.data.metrics[1].metricKey").value("ACTIVE_MINUTES"));

        Long battleId = jdbcTemplate.queryForObject("select id from battles", Long.class);
        Timestamp startsAt = jdbcTemplate.queryForObject("select starts_at from battles where id = ?", Timestamp.class, battleId);
        Timestamp endsAt = jdbcTemplate.queryForObject("select ends_at from battles where id = ?", Timestamp.class, battleId);
        org.assertj.core.api.Assertions.assertThat(Duration.between(startsAt.toInstant(), endsAt.toInstant()))
                .isEqualTo(Duration.ofDays(1));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.battleId").value(battleId))
                .andExpect(jsonPath("$.data.matchStatus").value("MATCHED"));

        Integer battleCount = jdbcTemplate.queryForObject("select count(*) from battles", Integer.class);
        Integer matchedQueueCount = jdbcTemplate.queryForObject("""
                select count(*)
                from battle_match_queue
                where status = 'MATCHED'
                  and matched_battle_id = ?
                """, Integer.class, battleId);
        org.assertj.core.api.Assertions.assertThat(battleCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(matchedQueueCount).isEqualTo(2);
        verify(notificationService, times(1)).sendBattleMatched(any(Battle.class), anyList());
    }

    @Test
    void matchExcludesInternalProbeAccountsFromWaitingQueue() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleRealPoolMe", "실사용자");
        TestUser probe = testUser("jenkins_probe_battle_pool", "프로브계정");
        TestUser realOpponent = testUser("battleRealOpponent", "실제상대");
        LocalDate today = KoreanTime.today();
        seedCompletedQuest(probe.userId(), today, "routine", healthProof(30, 4000, 3000, 200));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + probe.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + realOpponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("MATCHED"))
                .andExpect(jsonPath("$.data.participants[1].userId").value(realOpponent.userId()))
                .andExpect(jsonPath("$.data.participants[1].nickname").value("실제상대"));
    }

    @Test
    void matchIncludesManualCompletedQuestCountInBattleScore() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleManualMe", "수동완료");
        TestUser opponent = testUser("battleManualOpponent", "상대");
        LocalDate today = KoreanTime.today();
        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + opponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].userId").value(me.userId()))
                .andExpect(jsonPath("$.data.participants[0].score").value(0))
                .andExpect(jsonPath("$.data.participants[1].userId").value(opponent.userId()))
                .andExpect(jsonPath("$.data.participants[1].score").value(0))
                .andExpect(jsonPath("$.data.score.leadingUserId").doesNotExist());

        Long battleId = jdbcTemplate.queryForObject("select id from battles", Long.class);
        seedCompletedQuest(me.userId(), today, "routine", manualProofWithLargeMetrics());

        mockMvc.perform(get("/api/battles/{battleId}", battleId)
                        .header("Authorization", "Bearer " + me.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].score").value(100))
                .andExpect(jsonPath("$.data.participants[1].score").value(0))
                .andExpect(jsonPath("$.data.score.leadingUserId").value(me.userId()))
                .andExpect(jsonPath("$.data.metrics[5].metricKey").value("COMPLETED_QUESTS"))
                .andExpect(jsonPath("$.data.metrics[5].myValue").value("1개"));
    }

    @Test
    void battleUsesSyncedDailyHealthSummaryEvenWhenQuestWasManual() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleManualHealthMe", "수동건강");
        TestUser opponent = testUser("battleManualHealthOpponent", "상대");
        LocalDate today = KoreanTime.today();
        seedHealthDailySummary(me.userId(), today, 3000, 2000, 150, 20);
        seedHealthDailySummary(opponent.userId(), today, 1000, 500, 40, 5);

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + opponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].score").value(0));

        Long battleId = jdbcTemplate.queryForObject("select id from battles", Long.class);
        seedCompletedQuest(me.userId(), today, "routine", manualProofWithLargeMetrics());
        updateHealthDailySummary(me.userId(), today, 5000, 3500, 240, 35);

        mockMvc.perform(get("/api/battles/{battleId}", battleId)
                        .header("Authorization", "Bearer " + me.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].score").value(495))
                .andExpect(jsonPath("$.data.metrics[0].myValue").value("495점"))
                .andExpect(jsonPath("$.data.metrics[1].myValue").value("15분"))
                .andExpect(jsonPath("$.data.metrics[2].myValue").value("1500m"))
                .andExpect(jsonPath("$.data.metrics[3].myValue").value("2000걸음"))
                .andExpect(jsonPath("$.data.metrics[4].myValue").value("90kcal"))
                .andExpect(jsonPath("$.data.metrics[5].myValue").value("1개"))
                .andExpect(jsonPath("$.data.healthSync.required").value(false))
                .andExpect(jsonPath("$.data.healthSync.recommended").value(false))
                .andExpect(jsonPath("$.data.healthSync.staleAfterSeconds").value(1800))
                .andExpect(jsonPath("$.data.participants[0].latestHealthSyncedAt").exists())
                .andExpect(jsonPath("$.data.participants[1].latestHealthSyncedAt").exists());
    }

    @Test
    void matchWaitsWhenNoQueuedOpponentExists() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleSolo", "혼자");

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.participants.length()").value(1))
                .andExpect(jsonPath("$.data.participants[0].userId").value(me.userId()))
                .andExpect(jsonPath("$.data.healthSync.required").value(true))
                .andExpect(jsonPath("$.data.healthSync.recommended").value(true));
    }

    @Test
    void resultFinalizesEndedBattleAndHistoryReturnsIt() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleWinner", "승자");
        TestUser opponent = testUser("battleLoser", "패자");
        LocalDate today = KoreanTime.today();
        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + opponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk());
        Long battleId = jdbcTemplate.queryForObject("select id from battles", Long.class);
        Instant completedAt = KoreanTime.nowInstant().minus(Duration.ofMinutes(30));
        seedCompletedQuestAt(me.userId(), today, "routine", healthProof(30, 4000, 3000, 200), completedAt);
        seedCompletedQuestAt(opponent.userId(), today, "off_day", healthProof(10, 800, 300, 50), completedAt);
        jdbcTemplate.update(
                "update battles set starts_at = ?, ends_at = ? where id = ?",
                Timestamp.from(KoreanTime.nowInstant().minus(Duration.ofHours(2))),
                Timestamp.from(KoreanTime.nowInstant().minus(Duration.ofMinutes(1))),
                battleId
        );

        mockMvc.perform(get("/api/battles/{battleId}/result", battleId)
                        .header("Authorization", "Bearer " + me.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalized").value(true))
                .andExpect(jsonPath("$.data.result").value("WIN"))
                .andExpect(jsonPath("$.data.winnerUserId").value(me.userId()))
                .andExpect(jsonPath("$.data.rewardExp").value(30))
                .andExpect(jsonPath("$.data.rewardGold").value(15))
                .andExpect(jsonPath("$.data.myScore").value(980))
                .andExpect(jsonPath("$.data.opponentScore").value(367));

        org.assertj.core.api.Assertions.assertThat(expRewardCount(me.userId(), battleId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(expRewardCount(opponent.userId(), battleId)).isZero();
        org.assertj.core.api.Assertions.assertThat(currencyRewardCount(me.userId(), battleId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(currencyRewardCount(opponent.userId(), battleId)).isZero();
        org.assertj.core.api.Assertions.assertThat(walletBalance(me.userId())).isEqualTo(15);

        mockMvc.perform(get("/api/battles/history")
                        .header("Authorization", "Bearer " + me.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.battles[0].battleId").value(battleId))
                .andExpect(jsonPath("$.data.battles[0].result").value("WIN"))
                .andExpect(jsonPath("$.data.battles[0].opponent.userId").value(opponent.userId()));

        mockMvc.perform(get("/api/battles/{battleId}/result", battleId)
                        .header("Authorization", "Bearer " + me.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalized").value(true))
                .andExpect(jsonPath("$.data.rewardExp").value(30))
                .andExpect(jsonPath("$.data.rewardGold").value(15));

        mockMvc.perform(get("/api/battles/{battleId}/result", battleId)
                        .header("Authorization", "Bearer " + opponent.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalized").value(true))
                .andExpect(jsonPath("$.data.result").value("LOSS"))
                .andExpect(jsonPath("$.data.rewardExp").value(0))
                .andExpect(jsonPath("$.data.rewardGold").value(0));

        org.assertj.core.api.Assertions.assertThat(expRewardCount(me.userId(), battleId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(currencyRewardCount(me.userId(), battleId)).isEqualTo(1);

        verify(notificationService, times(1)).sendBattleResultReady(any(Battle.class), anyList());
    }

    @Test
    void drawBattleDoesNotIssueRewards() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser me = testUser("battleDrawMe", "무승부1");
        TestUser opponent = testUser("battleDrawOpponent", "무승부2");
        LocalDate today = KoreanTime.today();
        seedCompletedQuest(me.userId(), today, "routine", healthProof(20, 1000, 1000, 100));
        seedCompletedQuest(opponent.userId(), today, "routine", healthProof(20, 1000, 1000, 100));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + opponent.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        mockMvc.perform(post("/api/battles/match")
                        .header("Authorization", "Bearer " + me.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "mode": "DAILY"
                                }
                                """))
                .andExpect(status().isOk());
        Long battleId = jdbcTemplate.queryForObject("select id from battles", Long.class);
        jdbcTemplate.update(
                "update battles set ends_at = ? where id = ?",
                Timestamp.from(KoreanTime.nowInstant().minus(Duration.ofMinutes(1))),
                battleId
        );

        mockMvc.perform(get("/api/battles/{battleId}/result", battleId)
                        .header("Authorization", "Bearer " + me.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("DRAW"))
                .andExpect(jsonPath("$.data.rewardExp").value(0))
                .andExpect(jsonPath("$.data.rewardGold").value(0));

        org.assertj.core.api.Assertions.assertThat(expRewardCount(me.userId(), battleId)).isZero();
        org.assertj.core.api.Assertions.assertThat(expRewardCount(opponent.userId(), battleId)).isZero();
        org.assertj.core.api.Assertions.assertThat(currencyRewardCount(me.userId(), battleId)).isZero();
        org.assertj.core.api.Assertions.assertThat(currencyRewardCount(opponent.userId(), battleId)).isZero();
    }

    @Test
    void battleApisRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/battles/me/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private TestUser testUser(String loginId, String nickname) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", nickname));
        return new TestUser(user.getId(), jwtTokenService.issueTokenPair(user).accessToken());
    }

    private void seedCompletedQuest(Long userId, LocalDate questDate, String questType, String proofJson) {
        seedCompletedQuestAt(userId, questDate, questType, proofJson, KoreanTime.nowInstant());
    }

    private void seedCompletedQuestAt(Long userId, LocalDate questDate, String questType, String proofJson, Instant completedAt) {
        String escapedProofJson = proofJson.replace("'", "''");
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
                values (?, ?, ?, 'minutes', '배틀 테스트 퀘스트', '배틀 테스트용 완료 퀘스트입니다.', 10, 10, 'completed', false, 0, 0, ?, JSON '""" + escapedProofJson + """
                ', JSON '{}', CURRENT_TIMESTAMP)
                """, userId, questDate, questType, Timestamp.from(completedAt));
    }

    private void seedHealthDailySummary(Long userId,
                                        LocalDate summaryDate,
                                        int steps,
                                        int distanceMeters,
                                        int activeCalories,
                                        int exerciseMinutes) {
        jdbcTemplate.update("""
                insert into health_daily_summaries (
                    user_id,
                    summary_date,
                    steps,
                    distance_meters,
                    active_calories_kcal,
                    exercise_minutes,
                    sample_count,
                    synced_at,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, ?, ?, ?, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, summaryDate, steps, distanceMeters, activeCalories, exerciseMinutes);
    }

    private void updateHealthDailySummary(Long userId,
                                          LocalDate summaryDate,
                                          int steps,
                                          int distanceMeters,
                                          int activeCalories,
                                          int exerciseMinutes) {
        jdbcTemplate.update("""
                update health_daily_summaries
                set steps = ?,
                    distance_meters = ?,
                    active_calories_kcal = ?,
                    exercise_minutes = ?,
                    sample_count = 8,
                    synced_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                where user_id = ?
                  and summary_date = ?
                """, steps, distanceMeters, activeCalories, exerciseMinutes, userId, summaryDate);
    }

    private int expRewardCount(Long userId, Long battleId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from user_exp_logs
                where user_id = ?
                  and ref_type = 'battle_win'
                  and ref_id = ?
                """, Integer.class, userId, battleId);
    }

    private int currencyRewardCount(Long userId, Long battleId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from wallet_transactions
                where user_id = ?
                  and tx_type = 'competition_reward'
                  and ref_type = 'battle_win'
                  and ref_id = ?
                """, Integer.class, userId, battleId);
    }

    private int walletBalance(Long userId) {
        Integer balance = jdbcTemplate.queryForObject("""
                select balance_currency
                from wallets
                where user_id = ?
                """, Integer.class, userId);
        return balance == null ? 0 : balance;
    }

    private String healthProof(int minutes, int steps, int distanceMeters, int activeCalories) {
        return """
                {
                  "source": "health_data",
                  "verified": true,
                  "metrics": {
                    "exerciseMinutes": %d,
                    "steps": %d,
                    "distanceMeters": %d,
                    "activeCaloriesKcal": %d
                  }
                }
                """.formatted(minutes, steps, distanceMeters, activeCalories);
    }

    private String manualProofWithLargeMetrics() {
        return """
                {
                  "source": "manual",
                  "verified": false,
                  "completionType": "MANUAL",
                  "verificationStatus": "NOT_PROVIDED",
                  "battleEligible": false,
                  "metrics": {
                    "exerciseMinutes": 120,
                    "steps": 30000,
                    "distanceMeters": 30000,
                    "activeCaloriesKcal": 1000
                  }
                }
                """;
    }

    private record TestUser(Long userId, String accessToken) {
    }
}
