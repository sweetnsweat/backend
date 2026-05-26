package com.capstone.backend.story.dto;

import com.capstone.backend.story.entity.StoryPlayLog;
import com.capstone.backend.story.entity.StoryQuest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StoryChatMessageResponse(
        Integer id,
        String role,
        Integer chapterNum,
        Integer choiceId,
        Integer detailId,
        Integer unitIndex,
        String userMessage,
        String narrationText,
        String dialogueText,
        String outputText,
        String content,
        @JsonProperty("quest_id")
        Integer questId,
        @JsonProperty("workout_quest")
        Map<String, Object> workoutQuest,
        Instant createdAt
) {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    public static StoryChatMessageResponse from(StoryPlayLog log) {
        return new StoryChatMessageResponse(
                log.getId(),
                "story_log",
                log.getChapterNum(),
                log.getChoiceId(),
                log.getDetailId(),
                log.getUnitIndex(),
                log.getUserMessage(),
                log.getNarrationText(),
                log.getDialogueText(),
                log.getOutputText(),
                null,
                null,
                null,
                log.getCreatedAt()
        );
    }

    public static StoryChatMessageResponse from(StoryQuest quest) {
        return new StoryChatMessageResponse(
                quest.getId(),
                "workout_quest",
                quest.getChapterNum(),
                null,
                null,
                quest.getUnitIndex(),
                null,
                null,
                null,
                null,
                quest.getDescription(),
                quest.getId(),
                workoutQuest(quest),
                quest.getCreatedAt()
        );
    }

    private static Map<String, Object> workoutQuest(StoryQuest quest) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("title", quest.getTitle());
        node.put("description", quest.getDescription());
        node.put("source", quest.getSource());
        node.put("quests", parseQuestJson(quest.getQuestJson()));
        return node;
    }

    private static List<Object> parseQuestJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            Object parsed = OBJECT_MAPPER.readValue(rawJson, Object.class);
            if (parsed == null) {
                return List.of();
            }
            if (parsed instanceof List<?> list) {
                return new ArrayList<>(list);
            }
            return List.of(parsed);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
