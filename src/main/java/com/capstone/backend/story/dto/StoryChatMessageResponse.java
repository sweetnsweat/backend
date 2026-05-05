package com.capstone.backend.story.dto;

import com.capstone.backend.story.entity.StoryPlayLog;
import java.time.Instant;

public record StoryChatMessageResponse(
        Integer id,
        Integer chapterNum,
        Integer choiceId,
        Integer detailId,
        Integer unitIndex,
        String userMessage,
        String narrationText,
        String dialogueText,
        String outputText,
        Instant createdAt
) {
    public static StoryChatMessageResponse from(StoryPlayLog log) {
        return new StoryChatMessageResponse(
                log.getId(),
                log.getChapterNum(),
                log.getChoiceId(),
                log.getDetailId(),
                log.getUnitIndex(),
                log.getUserMessage(),
                log.getNarrationText(),
                log.getDialogueText(),
                log.getOutputText(),
                log.getCreatedAt()
        );
    }
}
