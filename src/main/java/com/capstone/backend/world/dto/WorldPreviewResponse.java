package com.capstone.backend.world.dto;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.entity.CharacterProfile;
import com.capstone.backend.story.entity.Scenario;
import com.capstone.backend.story.entity.StoryProgress;
import java.util.ArrayList;
import java.util.List;

public record WorldPreviewResponse(
        ScenarioResponse scenario,
        RankingResponse ranking,
        CharacterResponse representativeCharacter,
        List<CharacterResponse> characters,
        EntryResponse entry
) {
    private static final String METRIC_ACTIVE_CHAT_COUNT = "ACTIVE_CHAT_COUNT";

    public static WorldPreviewResponse from(Scenario scenario,
                                            List<String> genres,
                                            List<CharacterProfile> characters,
                                            StoryProgress progress,
                                            long score,
                                            MediaUrlResolver mediaUrlResolver) {
        List<CharacterResponse> characterResponses = characters.stream()
                .map(character -> CharacterResponse.from(character, mediaUrlResolver))
                .toList();
        CharacterResponse representativeCharacter = characterResponses.stream()
                .filter(CharacterResponse::representative)
                .findFirst()
                .orElseGet(() -> characterResponses.isEmpty() ? null : characterResponses.getFirst());

        return new WorldPreviewResponse(
                ScenarioResponse.from(scenario, genres, mediaUrlResolver),
                new RankingResponse(METRIC_ACTIVE_CHAT_COUNT, score),
                representativeCharacter,
                characterResponses,
                EntryResponse.from(scenario, progress)
        );
    }

    public record ScenarioResponse(
            Integer id,
            String title,
            String summary,
            String genre,
            List<String> genres,
            String thumbnailUrl,
            String worldImageUrl,
            String playerImageUrl,
            String playerDescription,
            boolean active
    ) {
        static ScenarioResponse from(Scenario scenario, List<String> genres, MediaUrlResolver mediaUrlResolver) {
            List<String> resolvedGenres = genres == null || genres.isEmpty()
                    ? splitWords(scenario.getGenre())
                    : List.copyOf(genres);
            return new ScenarioResponse(
                    scenario.getId(),
                    scenario.getTitle(),
                    scenario.getSummary(),
                    scenario.getGenre(),
                    resolvedGenres,
                    mediaUrlResolver.resolve(scenario.getThumbnailUrl()),
                    mediaUrlResolver.resolve(scenario.getWorldImageUrl()),
                    mediaUrlResolver.resolve(scenario.getPlayerImageUrl()),
                    scenario.getPlayerDescription(),
                    Boolean.TRUE.equals(scenario.getActive())
            );
        }
    }

    public record RankingResponse(
            String metric,
            long score
    ) {
    }

    public record CharacterResponse(
            Integer id,
            String name,
            String title,
            String type,
            String imageUrl,
            String quote,
            List<String> tags,
            boolean representative
    ) {
        static CharacterResponse from(CharacterProfile character, MediaUrlResolver mediaUrlResolver) {
            return new CharacterResponse(
                    character.getId(),
                    character.getName(),
                    character.getCharacterTitle(),
                    character.getCharacterType(),
                    mediaUrlResolver.resolve(character.getImageUrl()),
                    character.getMidStoryLine(),
                    parseTags(character.getTags()),
                    Boolean.TRUE.equals(character.getRepresentative())
            );
        }
    }

    public record EntryResponse(
            boolean canEnter,
            boolean hasProgress,
            String progressStatus
    ) {
        static EntryResponse from(Scenario scenario, StoryProgress progress) {
            return new EntryResponse(
                    Boolean.TRUE.equals(scenario.getActive()),
                    progress != null,
                    progress == null ? null : progress.getStatus()
            );
        }
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
