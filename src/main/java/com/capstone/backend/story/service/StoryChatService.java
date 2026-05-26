package com.capstone.backend.story.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.story.dto.StoryChatCharacterResponse;
import com.capstone.backend.story.dto.StoryChatDetailResponse;
import com.capstone.backend.story.dto.StoryChatListResponse;
import com.capstone.backend.story.dto.StoryChatMessageResponse;
import com.capstone.backend.story.dto.StoryChatSummaryResponse;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.StoryPlayLog;
import com.capstone.backend.story.entity.StoryProgress;
import com.capstone.backend.story.entity.StoryQuest;
import com.capstone.backend.story.repository.CharacterProfileRepository;
import com.capstone.backend.story.repository.StoryPlayLogRepository;
import com.capstone.backend.story.repository.StoryProgressRepository;
import com.capstone.backend.story.repository.StoryQuestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryChatService {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private final StoryProgressRepository storyProgressRepository;
    private final StoryPlayLogRepository storyPlayLogRepository;
    private final StoryQuestRepository storyQuestRepository;
    private final UserQuestRepository userQuestRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final MediaUrlResolver mediaUrlResolver;

    public StoryChatService(StoryProgressRepository storyProgressRepository,
                            StoryPlayLogRepository storyPlayLogRepository,
                            StoryQuestRepository storyQuestRepository,
                            UserQuestRepository userQuestRepository,
                            CharacterProfileRepository characterProfileRepository,
                            MediaUrlResolver mediaUrlResolver) {
        this.storyProgressRepository = storyProgressRepository;
        this.storyPlayLogRepository = storyPlayLogRepository;
        this.storyQuestRepository = storyQuestRepository;
        this.userQuestRepository = userQuestRepository;
        this.characterProfileRepository = characterProfileRepository;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional(readOnly = true)
    public StoryChatListResponse getChats(Long userId, int limit) {
        int normalizedLimit = clampLimit(limit);
        String userKey = String.valueOf(userId);
        List<StoryProgress> progresses = storyProgressRepository.findActiveChatsByUserKey(
                userKey,
                PageRequest.of(0, normalizedLimit)
        );
        Map<Integer, CharacterProfile> representativeCharacters = representativeCharactersByScenarioId(progresses);
        List<StoryChatSummaryResponse> chats = progresses.stream()
                .map(progress -> StoryChatSummaryResponse.from(
                        progress,
                        representativeCharacters.get(progress.getScenario().getId()),
                        mediaUrlResolver
                ))
                .toList();
        return new StoryChatListResponse(
                normalizedLimit,
                storyProgressRepository.countActiveChatsByUserKey(userKey),
                chats
        );
    }

    @Transactional(readOnly = true)
    public StoryChatDetailResponse getChat(Long userId, Integer scenarioId, int messageLimit) {
        StoryProgress progress = storyProgressRepository.findActiveChatByScenarioIdAndUserKey(
                        scenarioId,
                        String.valueOf(userId),
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STORY_CHAT_NOT_FOUND", "입장한 채팅방을 찾을 수 없습니다."));
        List<CharacterProfile> characters = characterProfileRepository.findByScenario_IdOrderByRepresentativeDescIdAsc(scenarioId);
        CharacterProfile representativeCharacter = characters.isEmpty() ? null : characters.getFirst();
        int normalizedMessageLimit = clampMessageLimit(messageLimit);
        String userKey = String.valueOf(userId);
        List<StoryPlayLog> logs = new ArrayList<>(storyPlayLogRepository.findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(
                userKey,
                scenarioId,
                PageRequest.of(0, normalizedMessageLimit)
        ));
        List<StoryQuest> quests = new ArrayList<>(storyQuestRepository.findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(
                userKey,
                scenarioId,
                PageRequest.of(0, normalizedMessageLimit)
        ));
        List<StoryQuest> visibleQuests = filterIncompleteStoryQuests(userId, quests);
        List<StoryChatMessageResponse> recentMessages = mergeMessages(logs, visibleQuests, normalizedMessageLimit);
        long messageTotalCount = storyPlayLogRepository.countByUserKeyAndScenarioId(userKey, scenarioId)
                + countVisibleStoryQuests(userId, userKey, scenarioId);

        return new StoryChatDetailResponse(
                StoryChatSummaryResponse.from(progress, representativeCharacter, mediaUrlResolver),
                characters.stream()
                        .map(character -> StoryChatCharacterResponse.from(character, mediaUrlResolver))
                        .toList(),
                normalizedMessageLimit,
                messageTotalCount,
                messageTotalCount > recentMessages.size(),
                recentMessages
        );
    }

    private List<StoryChatMessageResponse> mergeMessages(List<StoryPlayLog> logs, List<StoryQuest> quests, int limit) {
        List<StoryChatMessageResponse> messages = new ArrayList<>(logs.size() + quests.size());
        messages.addAll(logs.stream()
                .map(StoryChatMessageResponse::from)
                .toList());
        messages.addAll(quests.stream()
                .map(StoryChatMessageResponse::from)
                .toList());

        messages.sort(Comparator
                .comparing(StoryChatMessageResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StoryChatMessageResponse::id, Comparator.nullsLast(Comparator.naturalOrder())));
        if (messages.size() <= limit) {
            return List.copyOf(messages);
        }
        return List.copyOf(messages.subList(messages.size() - limit, messages.size()));
    }

    private long countVisibleStoryQuests(Long userId, String userKey, Integer scenarioId) {
        List<StoryQuest> quests = storyQuestRepository.findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(userKey, scenarioId);
        return filterIncompleteStoryQuests(userId, quests).size();
    }

    private List<StoryQuest> filterIncompleteStoryQuests(Long userId, List<StoryQuest> quests) {
        if (quests.isEmpty()) {
            return List.of();
        }

        Map<Integer, Set<Long>> questExternalIds = new LinkedHashMap<>();
        Set<Long> allExternalIds = new HashSet<>();
        for (StoryQuest quest : quests) {
            Set<Long> externalIds = extractExternalQuestIds(quest.getQuestJson());
            questExternalIds.put(quest.getId(), externalIds);
            allExternalIds.addAll(externalIds);
        }
        if (allExternalIds.isEmpty()) {
            return List.copyOf(quests);
        }

        Set<Long> completedQuestIds = new HashSet<>();
        userQuestRepository.findByUser_IdAndIdIn(userId, new ArrayList<>(allExternalIds)).stream()
                .filter(quest -> UserQuest.STATUS_COMPLETED.equals(quest.getStatus()))
                .map(UserQuest::getId)
                .forEach(completedQuestIds::add);

        return quests.stream()
                .filter(quest -> {
                    Set<Long> externalIds = questExternalIds.getOrDefault(quest.getId(), Set.of());
                    return externalIds.isEmpty() || externalIds.stream().noneMatch(completedQuestIds::contains);
                })
                .toList();
    }

    private Set<Long> extractExternalQuestIds(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Set.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawJson);
            Set<Long> ids = new HashSet<>();
            collectExternalQuestIds(root, ids);
            return ids;
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private void collectExternalQuestIds(JsonNode node, Set<Long> ids) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            addExternalQuestId(node.get("external_quest_id"), ids);
            addExternalQuestId(node.get("externalQuestId"), ids);
            node.fields().forEachRemaining(entry -> collectExternalQuestIds(entry.getValue(), ids));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectExternalQuestIds(child, ids));
        }
    }

    private void addExternalQuestId(JsonNode node, Set<Long> ids) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isIntegralNumber()) {
            ids.add(node.longValue());
            return;
        }
        if (node.isTextual()) {
            try {
                ids.add(Long.parseLong(node.asText()));
            } catch (NumberFormatException ignored) {
                // Ignore AI wrapper values that are not actual user_quests IDs.
            }
        }
    }

    private Map<Integer, CharacterProfile> representativeCharactersByScenarioId(List<StoryProgress> progresses) {
        List<Integer> scenarioIds = progresses.stream()
                .map(progress -> progress.getScenario().getId())
                .distinct()
                .toList();
        if (scenarioIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, CharacterProfile> result = new LinkedHashMap<>();
        for (CharacterProfile profile : characterProfileRepository.findByScenario_IdInOrderByScenario_IdAscRepresentativeDescIdAsc(scenarioIds)) {
            result.putIfAbsent(profile.getScenario().getId(), profile);
        }
        return result;
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private int clampMessageLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
