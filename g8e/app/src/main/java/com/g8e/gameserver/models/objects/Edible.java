package com.g8e.gameserver.models.objects;

public class Edible {
    int itemID;
    int healAmount;

    public Edible(int itemID, int healAmount) {
        this.itemID = itemID;
        this.healAmount = healAmount;
    }

    public int getItemID() {
        return itemID;
    }

    public int getHealAmount() {
        return healAmount;
    }

}
