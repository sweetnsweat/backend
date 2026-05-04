package com.capstone.backend.world.service;

import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;
import com.capstone.backend.story.entity.StoryProgress;
import com.capstone.backend.story.repository.CharacterProfileRepository;
import com.capstone.backend.story.repository.ScenarioRepository;
import com.capstone.backend.story.repository.StoryProgressRepository;
import com.capstone.backend.world.dto.WorldRankingListResponse;
import com.capstone.backend.world.dto.WorldRankingResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldService {

    private static final String METRIC_ACTIVE_CHAT_COUNT = "ACTIVE_CHAT_COUNT";

    private final StoryProgressRepository storyProgressRepository;
    private final ScenarioRepository scenarioRepository;
    private final CharacterProfileRepository characterProfileRepository;

    public WorldService(StoryProgressRepository storyProgressRepository,
                        ScenarioRepository scenarioRepository,
                        CharacterProfileRepository characterProfileRepository) {
        this.storyProgressRepository = storyProgressRepository;
        this.scenarioRepository = scenarioRepository;
        this.characterProfileRepository = characterProfileRepository;
    }

    @Transactional(readOnly = true)
    public WorldRankingListResponse getRankings(int limit) {
        List<StoryProgressRepository.WorldRankingRow> rows = storyProgressRepository.findWorldRankingRows(
                StoryProgress.STATUS_IN_PROGRESS,
                PageRequest.of(0, limit)
        );
        if (rows.isEmpty()) {
            return new WorldRankingListResponse(METRIC_ACTIVE_CHAT_COUNT, List.of());
        }

        List<Integer> scenarioIds = rows.stream()
                .map(StoryProgressRepository.WorldRankingRow::getScenarioId)
                .toList();
        Map<Integer, Scenario> scenariosById = scenarioRepository.findAllById(scenarioIds).stream()
                .collect(Collectors.toMap(Scenario::getId, Function.identity()));
        Map<Integer, CharacterProfile> representativeCharacters = representativeCharactersByScenarioId(scenarioIds);

        List<WorldRankingResponse> rankings = new java.util.ArrayList<>();
        int rank = 1;
        for (StoryProgressRepository.WorldRankingRow row : rows) {
            Scenario scenario = scenariosById.get(row.getScenarioId());
            if (scenario == null) {
                continue;
            }
            rankings.add(WorldRankingResponse.from(
                    rank++,
                    scenario,
                    representativeCharacters.get(row.getScenarioId()),
                    row.getScore()
            ));
        }
        return new WorldRankingListResponse(METRIC_ACTIVE_CHAT_COUNT, rankings);
    }

    private Map<Integer, CharacterProfile> representativeCharactersByScenarioId(List<Integer> scenarioIds) {
        Map<Integer, CharacterProfile> result = new LinkedHashMap<>();
        for (CharacterProfile profile : characterProfileRepository.findByScenario_IdInOrderByScenario_IdAscRepresentativeDescIdAsc(scenarioIds)) {
            Integer scenarioId = profile.getScenario().getId();
            result.putIfAbsent(scenarioId, profile);
        }
        return result;
    }
}
