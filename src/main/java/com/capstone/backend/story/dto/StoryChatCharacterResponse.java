package com.capstone.backend.story.dto;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.story.entity.CharacterProfile;
import java.util.ArrayList;
import java.util.List;

public record StoryChatCharacterResponse(
        Integer id,
        String name,
        String title,
        String type,
        String imageUrl,
        String quote,
        List<String> tags,
        boolean representative
) {
    public static StoryChatCharacterResponse from(CharacterProfile character, MediaUrlResolver mediaUrlResolver) {
        return new StoryChatCharacterResponse(
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

    static List<String> parseTags(String rawTags) {
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
