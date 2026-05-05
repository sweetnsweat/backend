package com.capstone.backend.story.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.dto.StoryChatCharacterResponse;
import com.capstone.backend.story.dto.StoryChatDetailResponse;
import com.capstone.backend.story.dto.StoryChatListResponse;
import com.capstone.backend.story.dto.StoryChatMessageResponse;
import com.capstone.backend.story.dto.StoryChatSummaryResponse;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.StoryPlayLog;
import com.capstone.backend.story.entity.StoryProgress;
import com.capstone.backend.story.repository.CharacterProfileRepository;
import com.capstone.backend.story.repository.StoryPlayLogRepository;
import com.capstone.backend.story.repository.StoryProgressRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryChatService {

    private final StoryProgressRepository storyProgressRepository;
    private final StoryPlayLogRepository storyPlayLogRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final MediaUrlResolver mediaUrlResolver;

    public StoryChatService(StoryProgressRepository storyProgressRepository,
                            StoryPlayLogRepository storyPlayLogRepository,
                            CharacterProfileRepository characterProfileRepository,
                            MediaUrlResolver mediaUrlResolver) {
        this.storyProgressRepository = storyProgressRepository;
        this.storyPlayLogRepository = storyPlayLogRepository;
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
        Collections.reverse(logs);
        long messageTotalCount = storyPlayLogRepository.countByUserKeyAndScenarioId(userKey, scenarioId);

        return new StoryChatDetailResponse(
                StoryChatSummaryResponse.from(progress, representativeCharacter, mediaUrlResolver),
                characters.stream()
                        .map(character -> StoryChatCharacterResponse.from(character, mediaUrlResolver))
                        .toList(),
                normalizedMessageLimit,
                messageTotalCount,
                messageTotalCount > logs.size(),
                logs.stream()
                        .map(StoryChatMessageResponse::from)
                        .toList()
        );
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
