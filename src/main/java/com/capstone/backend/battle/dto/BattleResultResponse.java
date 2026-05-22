package com.capstone.backend.battle.dto;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleResult;
import com.capstone.backend.battle.entity.BattleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BattleResultResponse(
        Long battleId,
        BattleMode mode,
        BattleStatus status,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Instant startsAt,
        Instant endsAt,
        boolean finalized,
        BattleResult result,
        Long winnerUserId,
        Integer myScore,
        Integer opponentScore,
        List<BattleParticipantResponse> participants,
        List<BattleMetricResponse> metrics
) {
}
