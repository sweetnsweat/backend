package com.capstone.backend.battle.dto;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleResult;
import java.time.Instant;
import java.time.LocalDate;

public record BattleHistoryItemResponse(
        Long battleId,
        BattleMode mode,
        BattleResult result,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Instant endedAt,
        BattleParticipantResponse opponent,
        Integer myScore,
        Integer opponentScore
) {
}
