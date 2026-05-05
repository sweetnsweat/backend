package com.capstone.backend.story.dto;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;
import com.capstone.backend.story.entity.StoryProgress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record StoryChatSummaryResponse(
        Integer progressId,
        Integer scenarioId,
        String scenarioTitle,
        String worldTitle,
        String summary,
        String genre,
        String thumbnailUrl,
        String worldImageUrl,
        String playerImageUrl,
        RepresentativeCharacterResponse representativeCharacter,
        String displayName,
        String imageUrl,
        String backgroundImageUrl,
        String status,
        Integer currentChapterNum,
        String phase,
        String lastMessage,
        Instant startedAt,
        Instant updatedAt,
        String historyEndpoint,
        String playEndpoint
) {
    public static StoryChatSummaryResponse from(StoryProgress progress,
                                                CharacterProfile representativeCharacter,
                                                MediaUrlResolver mediaUrlResolver) {
        Scenario scenario = progress.getScenario();
        String characterName = representativeCharacter == null ? null : representativeCharacter.getName();
        String characterImageUrl = representativeCharacter == null ? null : representativeCharacter.getImageUrl();
        return new StoryChatSummaryResponse(
                progress.getId(),
                scenario.getId(),
                scenario.getTitle(),
                scenario.getTitle(),
                scenario.getSummary(),
                scenario.getGenre(),
                mediaUrlResolver.resolve(scenario.getThumbnailUrl()),
                mediaUrlResolver.resolve(scenario.getWorldImageUrl()),
                mediaUrlResolver.resolve(scenario.getPlayerImageUrl()),
                RepresentativeCharacterResponse.from(representativeCharacter, mediaUrlResolver),
                firstNonBlank(characterName, scenario.getTitle()),
                mediaUrlResolver.resolve(firstNonBlank(characterImageUrl, scenario.getThumbnailUrl(), scenario.getWorldImageUrl(), scenario.getPlayerImageUrl())),
                mediaUrlResolver.resolve(firstNonBlank(scenario.getWorldImageUrl(), scenario.getThumbnailUrl(), characterImageUrl, scenario.getPlayerImageUrl())),
                progress.getStatus(),
                progress.getCurrentChapterNum(),
                progress.getPhase(),
                progress.getLastOutput(),
                progress.getCreatedAt(),
                progress.getUpdatedAt(),
                "/api/stories/play/history?scenario_id=" + scenario.getId(),
                "/api/stories/play"
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

    public record RepresentativeCharacterResponse(
            Integer id,
            String name,
            String title,
            String type,
            String imageUrl,
            String quote,
            List<String> tags
    ) {
        static RepresentativeCharacterResponse from(CharacterProfile character, MediaUrlResolver mediaUrlResolver) {
            if (character == null) {
                return null;
            }
            return new RepresentativeCharacterResponse(
                    character.getId(),
                    character.getName(),
                    character.getCharacterTitle(),
                    character.getCharacterType(),
                    mediaUrlResolver.resolve(character.getImageUrl()),
                    character.getMidStoryLine(),
                    parseTags(character.getTags())
            );
        }

        private static List<String> parseTags(String rawTags) {
            if (rawTags == null || rawTags.isBlank()) {
                return List.of();
            }
            String trimmed = rawTags.trim();
            if ("[]".equals(trimmed)) {
                return List.of();
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            List<String> tags = new ArrayList<>();
            for (String rawTag : trimmed.split(",")) {
                String tag = rawTag.trim();
                if (tag.startsWith("\"") && tag.endsWith("\"") && tag.length() >= 2) {
                    tag = tag.substring(1, tag.length() - 1);
                }
                if (!tag.isBlank()) {
                    tags.add(tag);
                }
            }
            return tags;
        }
    }
}
