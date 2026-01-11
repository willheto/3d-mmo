package com.g8e.gameserver.util;

public class CombatUtils {

    public static int getAttackDamage(int attackExperience, int strengthExperience, int enemyDefenceExperience,
            int accuracyBonus,
            int attackValue,
            int enemyDefenceBonus) {
        int strengthLevel = ExperienceUtils.getLevelByExp(strengthExperience);

        int effectiveStrengthLevel = strengthLevel + 9;

        int maximumHit = (effectiveStrengthLevel * (attackValue + 64) + 320) / 640;

        int attackLevel = ExperienceUtils.getLevelByExp(attackExperience);
        int effectiveAttackLevel = attackLevel + 9;
        int attackRoll = (effectiveAttackLevel * (accuracyBonus + 64));

        int enemyEffectiveDefenceLevel = ExperienceUtils.getLevelByExp(enemyDefenceExperience) + 9;
        int defenceRoll = enemyEffectiveDefenceLevel * (enemyDefenceBonus + 64);

        double hitChance;
        if (attackRoll > defenceRoll) {
            hitChance = 1 - ((defenceRoll + 2.0) / (2.0 * (attackRoll + 1)));
        } else {
            hitChance = attackRoll / (2.0 * (defenceRoll + 1.0));
        }

        boolean hit = Math.random() < hitChance;

        if (!hit) {
            return 0;
        } else {
            // random from 1 to maximum hit
            return (int) (1 + Math.random() * (maximumHit - 1));

        }

    }

}
