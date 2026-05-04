package com.capstone.backend.home.service;

import com.capstone.backend.home.dto.HomeWorldBannerListResponse;
import com.capstone.backend.home.dto.HomeWorldBannerResponse;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;
import com.capstone.backend.story.repository.CharacterProfileRepository;
import com.capstone.backend.story.repository.ScenarioRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {

    private final ScenarioRepository scenarioRepository;
    private final CharacterProfileRepository characterProfileRepository;

    public HomeService(ScenarioRepository scenarioRepository,
                       CharacterProfileRepository characterProfileRepository) {
        this.scenarioRepository = scenarioRepository;
        this.characterProfileRepository = characterProfileRepository;
    }

    @Transactional(readOnly = true)
    public HomeWorldBannerListResponse getWorldBanners(int limit) {
        List<Scenario> scenarios = scenarioRepository.findByActiveTrueOrderByIdDesc(PageRequest.of(0, limit));
        if (scenarios.isEmpty()) {
            return new HomeWorldBannerListResponse(List.of());
        }

        List<Integer> scenarioIds = scenarios.stream()
                .map(Scenario::getId)
                .toList();
        Map<Integer, CharacterProfile> representativeCharacters = representativeCharactersByScenarioId(scenarioIds);

        List<HomeWorldBannerResponse> slides = scenarios.stream()
                .map(scenario -> HomeWorldBannerResponse.from(scenario, representativeCharacters.get(scenario.getId())))
                .toList();
        return new HomeWorldBannerListResponse(slides);
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
