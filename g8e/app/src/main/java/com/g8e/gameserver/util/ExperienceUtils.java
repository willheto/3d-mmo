package com.g8e.gameserver.util;

public class ExperienceUtils {
    private static final int[] levelExperience = new int[99];

    static {
        int acc = 0;
        for (int i = 0; i < 99; i++) {
            int level = i + 1;
            int delta = (int) Math.floor(level + Math.pow(2.0, level / 7.0) * 300.0);
            acc += delta;
            levelExperience[i] = (int) (Math.floor(acc / 4.0) * 10) / 10;
        }
    }

    public static int getLevelByExp(int exp) {
        for (int i = 98; i >= 0; i--) {
            if (exp >= levelExperience[i]) {
                return Math.min(i + 2, 99);
            }
        }
        return 1;
    }

    public static int getExpByLevel(int level) {
        if (level < 2 || level > 99) {
            throw new IllegalArgumentException("Level must be between 2 and 99.");
        }
        return levelExperience[level - 2];
    }

    public static int calculateExperienceDifference(int level) {
        if (level < 2) {
            throw new IllegalArgumentException("Level must be 2 or higher.");
        }
        double exponentPart = Math.pow(2.0, (level - 1) / 7.0);
        double expressionValue = level - 1 + 300 * exponentPart;
        return (int) Math.floor((1.0 / 4) * expressionValue);
    }

    public static int getExperienceUntilNextLevel(int experience) {
        int currentLevel = getLevelByExp(experience);
        int experienceDifference = calculateExperienceDifference(currentLevel + 1);
        return experienceDifference - (experience - calculateExperienceDifference(currentLevel));
    }

}
