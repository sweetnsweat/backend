package com.capstone.backend.story.controller;

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
class StoryChatControllerTest {

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
        jdbcTemplate.update("delete from story_quests");
        jdbcTemplate.update("delete from story_play_logs");
        jdbcTemplate.update("delete from story_progress");
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
    void chatsReturnsCurrentUsersStartedStoryChats() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("chatUser", "encoded-password", "Chat User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        seedScenarios();
        seedCharacterProfiles();
        seedStoryProgress(user.getId());
        seedStoryPlayLogs(user.getId());

        mockMvc.perform(get("/api/stories/chats")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(50))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.chats.length()").value(2))
                .andExpect(jsonPath("$.data.chats[0].scenarioId").value(2))
                .andExpect(jsonPath("$.data.chats[0].scenarioTitle").value("하륜의 세계"))
                .andExpect(jsonPath("$.data.chats[0].displayName").value("하륜"))
                .andExpect(jsonPath("$.data.chats[0].imageUrl").value("http://localhost:8000/media/assets/character_haryun.png"))
                .andExpect(jsonPath("$.data.chats[0].backgroundImageUrl").value("http://localhost:8000/media/assets/world_2.png"))
                .andExpect(jsonPath("$.data.chats[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.chats[0].statusLabel").value("진행 중"))
                .andExpect(jsonPath("$.data.chats[0].currentChapterNum").value(2))
                .andExpect(jsonPath("$.data.chats[0].phase").value("choice"))
                .andExpect(jsonPath("$.data.chats[0].lastMessage").value("하륜의 마지막 응답"))
                .andExpect(jsonPath("$.data.chats[0].historyEndpoint").value("/api/stories/play/history?scenario_id=2"))
                .andExpect(jsonPath("$.data.chats[0].playEndpoint").value("/api/stories/play"))
                .andExpect(jsonPath("$.data.chats[1].scenarioId").value(1))
                .andExpect(jsonPath("$.data.chats[1].displayName").value("카이렌"))
                .andExpect(jsonPath("$.data.chats[1].status").value("STORY_COMPLETED"))
                .andExpect(jsonPath("$.data.chats[1].statusLabel").value("완료"));
    }

    @Test
    void chatReturnsEntryMetadataForSelectedStartedScenario() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("chatDetailUser", "encoded-password", "Chat Detail User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        seedScenarios();
        seedCharacterProfiles();
        seedStoryProgress(user.getId());
        seedStoryPlayLogs(user.getId());

        mockMvc.perform(get("/api/stories/chats/1")
                        .queryParam("messageLimit", "2")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chat.scenarioId").value(1))
                .andExpect(jsonPath("$.data.chat.scenarioTitle").value("카이렌의 세계"))
                .andExpect(jsonPath("$.data.chat.representativeCharacter.name").value("카이렌"))
                .andExpect(jsonPath("$.data.chat.representativeCharacter.tags[0]").value("황태자"))
                .andExpect(jsonPath("$.data.chat.lastMessage").value("카이렌의 마지막 응답"))
                .andExpect(jsonPath("$.data.chat.historyEndpoint").value("/api/stories/play/history?scenario_id=1"))
                .andExpect(jsonPath("$.data.characters.length()").value(2))
                .andExpect(jsonPath("$.data.characters[0].name").value("카이렌"))
                .andExpect(jsonPath("$.data.characters[0].representative").value(true))
                .andExpect(jsonPath("$.data.characters[1].name").value("엘리오라"))
                .andExpect(jsonPath("$.data.messageLimit").value(2))
                .andExpect(jsonPath("$.data.messageTotalCount").value(3))
                .andExpect(jsonPath("$.data.hasMoreMessages").value(true))
                .andExpect(jsonPath("$.data.recentMessages.length()").value(2))
                .andExpect(jsonPath("$.data.recentMessages[0].userMessage").value("두 번째 입력"))
                .andExpect(jsonPath("$.data.recentMessages[0].outputText").value("두 번째 응답"))
                .andExpect(jsonPath("$.data.recentMessages[1].userMessage").value("세 번째 입력"))
                .andExpect(jsonPath("$.data.recentMessages[1].dialogueText").value("세 번째 대사"));
    }

    @Test
    void chatIncludesWorkoutQuestMessagesInRecentMessages() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("chatQuestUser", "encoded-password", "Chat Quest User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        seedScenarios();
        seedCharacterProfiles();
        seedStoryProgress(user.getId());
        seedStoryPlayLogs(user.getId());
        seedStoryQuest(user.getId());

        mockMvc.perform(get("/api/stories/chats/1")
                        .queryParam("messageLimit", "10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageTotalCount").value(4))
                .andExpect(jsonPath("$.data.hasMoreMessages").value(false))
                .andExpect(jsonPath("$.data.recentMessages.length()").value(4))
                .andExpect(jsonPath("$.data.recentMessages[2].role").value("workout_quest"))
                .andExpect(jsonPath("$.data.recentMessages[2].quest_id").value(11))
                .andExpect(jsonPath("$.data.recentMessages[2].content").value("스토리용 운동 퀘스트 설명"))
                .andExpect(jsonPath("$.data.recentMessages[2].workout_quest.title").value("스토리 퀘스트"))
                .andExpect(jsonPath("$.data.recentMessages[2].workout_quest.source").value("external_quest_today"))
                .andExpect(jsonPath("$.data.recentMessages[2].workout_quest.quests[0].external_quest_id").value(1321))
                .andExpect(jsonPath("$.data.recentMessages[3].role").value("story_log"))
                .andExpect(jsonPath("$.data.recentMessages[3].userMessage").value("세 번째 입력"));
    }

    @Test
    void chatDoesNotIncludeCompletedWorkoutQuestMessages() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("chatCompletedQuestUser", "encoded-password", "Chat Completed Quest User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        seedScenarios();
        seedCharacterProfiles();
        seedStoryProgress(user.getId());
        seedStoryPlayLogs(user.getId());
        seedCompletedUserQuest(user.getId(), 1321L);
        seedStoryQuest(user.getId());

        mockMvc.perform(get("/api/stories/chats/1")
                        .queryParam("messageLimit", "10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageTotalCount").value(3))
                .andExpect(jsonPath("$.data.hasMoreMessages").value(false))
                .andExpect(jsonPath("$.data.recentMessages.length()").value(3))
                .andExpect(jsonPath("$.data.recentMessages[0].role").value("story_log"))
                .andExpect(jsonPath("$.data.recentMessages[1].role").value("story_log"))
                .andExpect(jsonPath("$.data.recentMessages[2].role").value("story_log"));
    }

    @Test
    void chatReturnsNotFoundWhenUserHasNotStartedScenario() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("chatNotFoundUser", "encoded-password", "Chat Not Found User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        seedScenarios();
        seedCharacterProfiles();

        mockMvc.perform(get("/api/stories/chats/1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORY_CHAT_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/stories/chats/1"));
    }

    @Test
    void chatsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/stories/chats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/stories/chats"));
    }

    private void seedScenarios() {
        jdbcTemplate.update("""
                insert into scenarios (
                    id,
                    title,
                    summary,
                    genre,
                    thumbnail_url,
                    world_image_url,
                    player_image_url,
                    player_description,
                    is_active
                )
                values
                    (1, '카이렌의 세계', '요약 1', '로맨스 판타지', '/media/assets/thumb_1.png', '/media/assets/world_1.png', '/media/assets/player_1.png', '플레이어 설명 1', true),
                    (2, '하륜의 세계', '요약 2', '무협', '/media/assets/thumb_2.png', '/media/assets/world_2.png', '/media/assets/player_2.png', '플레이어 설명 2', true),
                    (3, '비활성 세계관', '요약 3', '테스트', '/media/assets/thumb_3.png', '/media/assets/world_3.png', '/media/assets/player_3.png', '플레이어 설명 3', false)
                """);
    }

    private void seedCharacterProfiles() {
        jdbcTemplate.update("""
                insert into character_profiles (id, scenario_id, name, character_title, character_type, image_url, mid_story_line, tags, is_representative)
                values
                    (1, 1, '카이렌', '황태자', 'main', '/media/assets/character_kairen.png', '대사 1', '["황태자", "판타지"]', true),
                    (2, 1, '엘리오라', '대사제', 'supporting', '/media/assets/character_eliora.png', '대사 1-2', '["대사제"]', false),
                    (3, 2, '하륜', '라이벌', 'main', '/media/assets/character_haryun.png', '대사 2', '["라이벌", "무협"]', true),
                    (4, 3, '비활성', '테스트', 'main', '/media/assets/character_inactive.png', '대사 3', '["비활성"]', true)
                """);
    }

    private void seedStoryProgress(Long userId) {
        jdbcTemplate.update("""
                insert into story_progress (
                    id,
                    user_key,
                    scenario_id,
                    current_chapter_num,
                    status,
                    phase,
                    last_output,
                    created_at,
                    updated_at
                )
                values
                    (1, ?, 1, 1, 'STORY_COMPLETED', 'intro', '카이렌의 마지막 응답', timestamp '2026-05-05 10:00:00', timestamp '2026-05-05 10:10:00'),
                    (2, ?, 2, 2, 'IN_PROGRESS', 'choice', '하륜의 마지막 응답', timestamp '2026-05-05 11:00:00', timestamp '2026-05-05 11:10:00'),
                    (3, '9999', 1, 1, 'IN_PROGRESS', 'intro', '다른 사용자 응답', timestamp '2026-05-05 12:00:00', timestamp '2026-05-05 12:10:00'),
                    (4, ?, 3, 1, 'IN_PROGRESS', 'intro', '비활성 응답', timestamp '2026-05-05 13:00:00', timestamp '2026-05-05 13:10:00')
                """, String.valueOf(userId), String.valueOf(userId), String.valueOf(userId));
    }

    private void seedStoryPlayLogs(Long userId) {
        jdbcTemplate.update("""
                insert into story_play_logs (
                    id,
                    user_key,
                    scenario_id,
                    chapter_num,
                    choice_id,
                    detail_id,
                    unit_index,
                    user_message,
                    narration_text,
                    dialogue_text,
                    output_text,
                    created_at
                )
                values
                    (1, ?, 1, 1, null, null, 1, '첫 번째 입력', '첫 번째 서술', '첫 번째 대사', '첫 번째 응답', timestamp '2026-05-05 10:01:00'),
                    (2, ?, 1, 1, null, null, 2, '두 번째 입력', '두 번째 서술', '두 번째 대사', '두 번째 응답', timestamp '2026-05-05 10:02:00'),
                    (3, ?, 1, 1, null, null, 3, '세 번째 입력', '세 번째 서술', '세 번째 대사', '세 번째 응답', timestamp '2026-05-05 10:03:00'),
                    (4, ?, 2, 2, null, null, 1, '하륜 입력', '하륜 서술', '하륜 대사', '하륜 응답', timestamp '2026-05-05 11:01:00'),
                    (5, '9999', 1, 1, null, null, 1, '다른 사용자 입력', '다른 사용자 서술', '다른 사용자 대사', '다른 사용자 응답', timestamp '2026-05-05 12:01:00')
                """, String.valueOf(userId), String.valueOf(userId), String.valueOf(userId), String.valueOf(userId));
    }

    private void seedStoryQuest(Long userId) {
        jdbcTemplate.update("""
                insert into story_quests (
                    id,
                    user_key,
                    scenario_id,
                    chapter_num,
                    unit_index,
                    quest_date,
                    source,
                    title,
                    description,
                    quest_json,
                    created_at,
                    updated_at
                )
                values (
                    11,
                    ?,
                    1,
                    1,
                    2,
                    '2026-05-05',
                    'external_quest_today',
                    '스토리 퀘스트',
                    '스토리용 운동 퀘스트 설명',
                    '[{"quest_name":"깨진 독 훈련","external_quest_id":1321,"target":"1세트 / 1분"}]',
                    timestamp '2026-05-05 10:02:30',
                    timestamp '2026-05-05 10:02:30'
                )
                """, String.valueOf(userId));
    }

    private void seedCompletedUserQuest(Long userId, Long questId) {
        jdbcTemplate.update("""
                insert into user_quests (
                    id,
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
                values (
                    ?,
                    ?,
                    '2026-05-05',
                    'routine',
                    'routine',
                    '완료된 스토리 퀘스트',
                    '이미 완료된 운동 퀘스트입니다.',
                    1,
                    1,
                    'completed',
                    false,
                    0,
                    0,
                    timestamp '2026-05-05 10:02:40',
                    JSON '{}',
                    JSON '{}',
                    timestamp '2026-05-05 10:02:00'
                )
                """, questId, userId);
    }
}
