package com.capstone.backend.world.service;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;
import com.capstone.backend.story.entity.ScenarioGenre;
import com.capstone.backend.story.entity.StoryProgress;
import com.capstone.backend.story.repository.CharacterProfileRepository;
import com.capstone.backend.story.repository.ScenarioGenreRepository;
import com.capstone.backend.story.repository.ScenarioRepository;
import com.capstone.backend.story.repository.StoryProgressRepository;
import com.capstone.backend.world.dto.WorldRankingDetailResponse;
import com.capstone.backend.world.dto.WorldRankingListResponse;
import com.capstone.backend.world.dto.WorldRankingPageResponse;
import com.capstone.backend.world.dto.WorldRankingResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldService {

    private static final String METRIC_ACTIVE_CHAT_COUNT = "ACTIVE_CHAT_COUNT";

    private final StoryProgressRepository storyProgressRepository;
    private final ScenarioRepository scenarioRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final ScenarioGenreRepository scenarioGenreRepository;
    private final MediaUrlResolver mediaUrlResolver;

    public WorldService(StoryProgressRepository storyProgressRepository,
                        ScenarioRepository scenarioRepository,
                        CharacterProfileRepository characterProfileRepository,
                        ScenarioGenreRepository scenarioGenreRepository,
                        MediaUrlResolver mediaUrlResolver) {
        this.storyProgressRepository = storyProgressRepository;
        this.scenarioRepository = scenarioRepository;
        this.characterProfileRepository = characterProfileRepository;
        this.scenarioGenreRepository = scenarioGenreRepository;
        this.mediaUrlResolver = mediaUrlResolver;
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
                    row.getScore(),
                    mediaUrlResolver
            ));
        }
        return new WorldRankingListResponse(METRIC_ACTIVE_CHAT_COUNT, rankings);
    }

    @Transactional(readOnly = true)
    public WorldRankingPageResponse getFullRankings(String genre, String keyword, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = clampSize(size);
        String normalizedGenre = normalizeFilter(genre);
        String normalizedKeyword = normalizeFilter(keyword);
        Page<StoryProgressRepository.WorldRankingRow> rowPage = storyProgressRepository.findWorldRankingFullRows(
                StoryProgress.STATUS_IN_PROGRESS,
                normalizedGenre,
                normalizedKeyword,
                PageRequest.of(normalizedPage, normalizedSize)
        );
        if (rowPage.isEmpty()) {
            return new WorldRankingPageResponse(
                    METRIC_ACTIVE_CHAT_COUNT,
                    normalizedGenre,
                    normalizedKeyword,
                    rowPage.getNumber(),
                    rowPage.getSize(),
                    rowPage.getTotalElements(),
                    rowPage.getTotalPages(),
                    rowPage.isFirst(),
                    rowPage.isLast(),
                    rowPage.hasNext(),
                    rowPage.hasNext() ? rowPage.getNumber() + 1 : null,
                    List.of()
            );
        }

        List<Integer> scenarioIds = rowPage.getContent().stream()
                .map(StoryProgressRepository.WorldRankingRow::getScenarioId)
                .toList();
        Map<Integer, Scenario> scenariosById = scenarioRepository.findAllById(scenarioIds).stream()
                .collect(Collectors.toMap(Scenario::getId, Function.identity()));
        Map<Integer, CharacterProfile> representativeCharacters = representativeCharactersByScenarioId(scenarioIds);
        Map<Integer, List<String>> genresByScenarioId = genresByScenarioId(scenarioIds);

        List<WorldRankingDetailResponse> rankings = new ArrayList<>();
        int rank = rowPage.getNumber() * rowPage.getSize() + 1;
        for (StoryProgressRepository.WorldRankingRow row : rowPage.getContent()) {
            Scenario scenario = scenariosById.get(row.getScenarioId());
            if (scenario == null) {
                continue;
            }
            rankings.add(WorldRankingDetailResponse.from(
                    rank++,
                    scenario,
                    genresByScenarioId.getOrDefault(row.getScenarioId(), List.of()),
                    representativeCharacters.get(row.getScenarioId()),
                    row.getScore() == null ? 0 : row.getScore(),
                    mediaUrlResolver
            ));
        }

        return new WorldRankingPageResponse(
                METRIC_ACTIVE_CHAT_COUNT,
                normalizedGenre,
                normalizedKeyword,
                rowPage.getNumber(),
                rowPage.getSize(),
                rowPage.getTotalElements(),
                rowPage.getTotalPages(),
                rowPage.isFirst(),
                rowPage.isLast(),
                rowPage.hasNext(),
                rowPage.hasNext() ? rowPage.getNumber() + 1 : null,
                rankings
        );
    }

    private Map<Integer, CharacterProfile> representativeCharactersByScenarioId(List<Integer> scenarioIds) {
        Map<Integer, CharacterProfile> result = new LinkedHashMap<>();
        for (CharacterProfile profile : characterProfileRepository.findByScenario_IdInOrderByScenario_IdAscRepresentativeDescIdAsc(scenarioIds)) {
            Integer scenarioId = profile.getScenario().getId();
            result.putIfAbsent(scenarioId, profile);
        }
        return result;
    }

    private Map<Integer, List<String>> genresByScenarioId(List<Integer> scenarioIds) {
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        for (ScenarioGenre genre : scenarioGenreRepository.findByScenario_IdInOrderByScenario_IdAscSeqAscIdAsc(scenarioIds)) {
            Integer scenarioId = genre.getScenario().getId();
            result.computeIfAbsent(scenarioId, ignored -> new ArrayList<>()).add(genre.getGenreName());
        }
        return result;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value) || "전체".equals(value)) {
            return null;
        }
        return value.trim();
    }

    private int clampSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }
}
