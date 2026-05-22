package com.capstone.backend.reward.policy;

import com.capstone.backend.battle.entity.BattleMode;

public final class BattleRewardPolicy {

    private BattleRewardPolicy() {
    }

    public static BattleReward win(BattleMode mode) {
        if (BattleMode.WEEKLY.equals(mode)) {
            return new BattleReward(100, 50);
        }
        return new BattleReward(30, 15);
    }

    public record BattleReward(int exp, int currency) {
    }
}
