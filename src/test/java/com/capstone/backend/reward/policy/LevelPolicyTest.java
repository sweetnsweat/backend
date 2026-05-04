package com.capstone.backend.reward.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LevelPolicyTest {

    @Test
    void levelUsesCumulativeExpCurve() {
        assertThat(LevelPolicy.totalExpForLevel(1)).isZero();
        assertThat(LevelPolicy.totalExpForLevel(2)).isEqualTo(100);
        assertThat(LevelPolicy.totalExpForLevel(3)).isEqualTo(300);
        assertThat(LevelPolicy.totalExpForLevel(5)).isEqualTo(1_000);

        assertThat(LevelPolicy.levelForTotalExp(0)).isEqualTo(1);
        assertThat(LevelPolicy.levelForTotalExp(99)).isEqualTo(1);
        assertThat(LevelPolicy.levelForTotalExp(100)).isEqualTo(2);
        assertThat(LevelPolicy.levelForTotalExp(300)).isEqualTo(3);
    }

    @Test
    void currentAndRemainingExpAreRelativeToCurrentLevel() {
        assertThat(LevelPolicy.currentLevelExp(420)).isEqualTo(120);
        assertThat(LevelPolicy.nextLevelRequiredExp(420)).isEqualTo(300);
        assertThat(LevelPolicy.nextLevelRemainingExp(420)).isEqualTo(180);
    }
}
