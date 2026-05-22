package com.capstone.backend.battle.dto;

import com.capstone.backend.battle.entity.BattleMode;
import jakarta.validation.constraints.NotNull;

public record BattleMatchRequest(
        @NotNull BattleMode mode
) {
}
