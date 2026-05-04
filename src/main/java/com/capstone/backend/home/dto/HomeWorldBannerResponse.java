package com.capstone.backend.home.dto;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;

public record HomeWorldBannerResponse(
        Integer scenarioId,
        String worldTitle,
        String genre,
        String summary,
        String imageUrl,
        String backgroundImageUrl,
        String representativeCharacterName,
        String representativeCharacterTitle,
        String headline,
        String quote
) {
    public static HomeWorldBannerResponse from(Scenario scenario,
                                               CharacterProfile representativeCharacter,
                                               MediaUrlResolver mediaUrlResolver) {
        String characterImageUrl = representativeCharacter == null ? null : representativeCharacter.getImageUrl();
        String characterName = representativeCharacter == null ? null : representativeCharacter.getName();
        String characterTitle = representativeCharacter == null ? null : representativeCharacter.getCharacterTitle();
        String quote = representativeCharacter == null ? null : representativeCharacter.getMidStoryLine();
        String headline = firstNonBlank(characterName, scenario.getTitle());

        return new HomeWorldBannerResponse(
                scenario.getId(),
                scenario.getTitle(),
                scenario.getGenre(),
                scenario.getSummary(),
                mediaUrlResolver.resolve(firstNonBlank(characterImageUrl, scenario.getThumbnailUrl(), scenario.getWorldImageUrl())),
                mediaUrlResolver.resolve(firstNonBlank(scenario.getWorldImageUrl(), scenario.getThumbnailUrl(), characterImageUrl)),
                characterName,
                characterTitle,
                headline,
                quote
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
