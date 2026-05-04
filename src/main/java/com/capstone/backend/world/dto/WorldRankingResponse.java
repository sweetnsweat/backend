package com.capstone.backend.world.dto;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;

public record WorldRankingResponse(
        int rank,
        Integer scenarioId,
        String worldTitle,
        String displayName,
        String imageUrl,
        long score
) {
    public static WorldRankingResponse from(int rank,
                                            Scenario scenario,
                                            CharacterProfile representativeCharacter,
                                            long score,
                                            MediaUrlResolver mediaUrlResolver) {
        String characterName = representativeCharacter == null ? null : representativeCharacter.getName();
        String characterImageUrl = representativeCharacter == null ? null : representativeCharacter.getImageUrl();

        return new WorldRankingResponse(
                rank,
                scenario.getId(),
                scenario.getTitle(),
                firstNonBlank(characterName, scenario.getTitle()),
                mediaUrlResolver.resolve(firstNonBlank(characterImageUrl, scenario.getThumbnailUrl(), scenario.getWorldImageUrl())),
                score
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
