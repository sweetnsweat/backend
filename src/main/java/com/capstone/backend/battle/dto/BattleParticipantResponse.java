package com.capstone.backend.battle.dto;

import com.capstone.backend.battle.entity.BattleResult;
import java.time.Instant;

public record BattleParticipantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        boolean me,
        Integer score,
        BattleResult result,
        Instant latestHealthSyncedAt
) {
}
