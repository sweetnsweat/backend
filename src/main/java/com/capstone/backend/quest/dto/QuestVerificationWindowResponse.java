package com.capstone.backend.quest.dto;

import java.time.Instant;

public record QuestVerificationWindowResponse(
        Instant startTime,
        Instant endTime
) {
}
