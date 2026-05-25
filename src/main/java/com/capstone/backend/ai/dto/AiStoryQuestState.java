package com.capstone.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record AiStoryQuestState(
        @JsonProperty("quest_date")
        LocalDate questDate,
        @JsonProperty("daily_quest_limit")
        int dailyQuestLimit,
        @JsonProperty("today_quest_issued")
        boolean todayQuestIssued,
        @JsonProperty("today_quest_completed")
        boolean todayQuestCompleted,
        @JsonProperty("today_quest_skipped")
        boolean todayQuestSkipped,
        @JsonProperty("can_issue_today_quest")
        boolean canIssueTodayQuest,
        @JsonProperty("today_quest_id")
        Long todayQuestId,
        @JsonProperty("today_quest_status")
        String todayQuestStatus,
        @JsonProperty("today_quest_completion_type")
        String todayQuestCompletionType
) {
}
