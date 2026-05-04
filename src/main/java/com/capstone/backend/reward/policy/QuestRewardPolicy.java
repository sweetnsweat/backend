package com.capstone.backend.reward.policy;

import com.capstone.backend.quest.entity.UserQuest;

public final class QuestRewardPolicy {

    private QuestRewardPolicy() {
    }

    public static QuestReward routine(Integer estimatedMinutes) {
        int minutes = estimatedMinutes == null ? 30 : estimatedMinutes;
        int exp = clamp(roundToNearestFive(minutes), 20, 60);
        return new QuestReward(exp, currencyFromExp(exp));
    }

    public static QuestReward offDay(Integer targetMinutes) {
        int minutes = targetMinutes == null ? 15 : targetMinutes;
        int exp = clamp(roundToNearestFive(minutes), 10, 25);
        return new QuestReward(exp, currencyFromExp(exp));
    }

    public static QuestReward recovery() {
        return new QuestReward(10, 5);
    }

    public static QuestReward forQuest(String questType, Integer targetValue, Integer estimatedMinutes) {
        return switch (questType == null ? "" : questType) {
            case UserQuest.TYPE_ROUTINE -> routine(estimatedMinutes);
            case UserQuest.TYPE_RECOVERY -> recovery();
            default -> offDay(targetValue);
        };
    }

    private static int currencyFromExp(int exp) {
        return Math.max(5, roundToNearestFive(Math.round(exp / 2.0f)));
    }

    private static int roundToNearestFive(int value) {
        return Math.round(value / 5.0f) * 5;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record QuestReward(int exp, int currency) {
    }
}
