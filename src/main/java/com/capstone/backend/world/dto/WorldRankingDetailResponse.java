package com.capstone.backend.world.dto;

import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;
import java.util.ArrayList;
import java.util.List;

public record WorldRankingDetailResponse(
        int rank,
        Integer scenarioId,
        String scenarioTitle,
        String worldTitle,
        String summary,
        String genre,
        List<String> genres,
        String thumbnailUrl,
        String worldImageUrl,
        String playerImageUrl,
        String playerDescription,
        RepresentativeCharacterResponse representativeCharacter,
        String displayName,
        String imageUrl,
        String backgroundImageUrl,
        long score
) {
    public static WorldRankingDetailResponse from(int rank,
                                                  Scenario scenario,
                                                  List<String> genres,
                                                  CharacterProfile representativeCharacter,
                                                  long score) {
        String characterName = representativeCharacter == null ? null : representativeCharacter.getName();
        String characterImageUrl = representativeCharacter == null ? null : representativeCharacter.getImageUrl();
        List<String> resolvedGenres = genres == null || genres.isEmpty()
                ? splitWords(scenario.getGenre())
                : List.copyOf(genres);

        return new WorldRankingDetailResponse(
                rank,
                scenario.getId(),
                scenario.getTitle(),
                scenario.getTitle(),
                scenario.getSummary(),
                scenario.getGenre(),
                resolvedGenres,
                scenario.getThumbnailUrl(),
                scenario.getWorldImageUrl(),
                scenario.getPlayerImageUrl(),
                scenario.getPlayerDescription(),
                RepresentativeCharacterResponse.from(representativeCharacter),
                firstNonBlank(characterName, scenario.getTitle()),
                firstNonBlank(characterImageUrl, scenario.getThumbnailUrl(), scenario.getWorldImageUrl(), scenario.getPlayerImageUrl()),
                firstNonBlank(scenario.getWorldImageUrl(), scenario.getThumbnailUrl(), characterImageUrl, scenario.getPlayerImageUrl()),
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

    private static List<String> splitWords(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (String word : value.trim().split("\\s+")) {
            if (!word.isBlank() && !words.contains(word)) {
                words.add(word);
            }
        }
        return words;
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
        static RepresentativeCharacterResponse from(CharacterProfile character) {
            if (character == null) {
                return null;
            }
            return new RepresentativeCharacterResponse(
                    character.getId(),
                    character.getName(),
                    character.getCharacterTitle(),
                    character.getCharacterType(),
                    character.getImageUrl(),
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
