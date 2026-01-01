package com.g8e.gameserver.models.objects;

public class Wieldable {
    int itemID;
    int accuracyBonus;
    int strengthBonus;
    int defenceBonus;
    int attackSpeed;
    String type;

    public Wieldable(int itemID, int accuracyBonus, int strengthBonus, int defenceBonus, int attackSpeed, String type) {
        this.itemID = itemID;
        this.accuracyBonus = accuracyBonus;
        this.strengthBonus = strengthBonus;
        this.defenceBonus = defenceBonus;
        this.attackSpeed = attackSpeed;
        this.type = type;

    }

    public int getItemID() {
        return itemID;
    }

    public int getAccuracyBonus() {
        return accuracyBonus;
    }

    public int getStrengthBonus() {
        return strengthBonus;
    }

    public int getDefenceBonus() {
        return defenceBonus;
    }

    public int getAttackSpeed() {
        return attackSpeed;
    }

    public String getType() {
        return type;
    }
}
