package com.capstone.backend.reward.policy;

import com.capstone.backend.reward.policy.QuestRewardPolicy.QuestReward;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestRewardPolicyTest {

    @Test
    void routineRewardUsesEstimatedMinutesWithFloorAndCap() {
        assertThat(QuestRewardPolicy.routine(15)).isEqualTo(new QuestReward(20, 10));
        assertThat(QuestRewardPolicy.routine(30)).isEqualTo(new QuestReward(30, 15));
        assertThat(QuestRewardPolicy.routine(45)).isEqualTo(new QuestReward(45, 25));
        assertThat(QuestRewardPolicy.routine(70)).isEqualTo(new QuestReward(60, 30));
    }

    @Test
    void offDayAndRecoveryRewardsStayLowerThanRoutineRewards() {
        assertThat(QuestRewardPolicy.offDay(15)).isEqualTo(new QuestReward(15, 10));
        assertThat(QuestRewardPolicy.offDay(60)).isEqualTo(new QuestReward(25, 15));
        assertThat(QuestRewardPolicy.recovery()).isEqualTo(new QuestReward(10, 5));
    }
}
