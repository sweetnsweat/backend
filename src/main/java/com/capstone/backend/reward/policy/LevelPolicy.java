package com.capstone.backend.reward.policy;

public final class LevelPolicy {

    private static final int MIN_LEVEL = 1;

    private LevelPolicy() {
    }

    public static int levelForTotalExp(int totalExp) {
        int safeTotalExp = Math.max(0, totalExp);
        int level = MIN_LEVEL;
        while (safeTotalExp >= totalExpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    public static int totalExpForLevel(int level) {
        int safeLevel = Math.max(MIN_LEVEL, level);
        return 50 * safeLevel * (safeLevel - 1);
    }

    public static int currentLevelExp(int totalExp) {
        int level = levelForTotalExp(totalExp);
        return Math.max(0, totalExp - totalExpForLevel(level));
    }

    public static int nextLevelRequiredExp(int totalExp) {
        int level = levelForTotalExp(totalExp);
        return totalExpForLevel(level + 1) - totalExpForLevel(level);
    }

    public static int nextLevelRemainingExp(int totalExp) {
        return nextLevelRequiredExp(totalExp) - currentLevelExp(totalExp);
    }
}
