package com.capstone.backend.quest.dto;

import java.util.Map;

public record CompleteQuestRequest(
        Integer progressValue,
        Map<String, Object> proof
) {
}
