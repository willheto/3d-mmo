package com.g8e.db.models;

public class DBPlayer {
    private int player_id;
    private int account_id;
    private int world_x;
    private int world_y;
    private Integer weapon = null;
    private Integer shield = null;
    private Integer helmet = null;
    private Integer body_armor = null;
    private Integer leg_armor = null;
    private Integer gloves = null;
    private Integer boots = null;
    private Integer neckwear = null;
    private Integer ring = null;
    private int[] inventory;
    private int[] inventoryAmounts;
    private int[] quest_progress;
    private int attack_experience;
    private int strength_experience;
    private int defence_experience;
    private int hitpoints_experience;
    private int magic_experience;
    private int skin_color;
    private int hair_color;
    private int shirt_color;
    private int pants_color;

    public DBPlayer(int player_id, int account_id, int skin_color, int hair_color, int shirt_color, int pants_color,
            int world_x,
            int world_y, Integer weapon, Integer shield, Integer helmet, Integer body_armor, Integer leg_armor,
            Integer gloves, Integer boots, Integer neckwear, Integer ring, int[] inventory, int[] inventoryAmounts,
            int[] quest_progress,
            int attack_experience, int strength_experience, int defence_experience, int hitpoints_experience,
            int magic_experience) {
        this.player_id = player_id;
        this.account_id = account_id;
        this.world_x = world_x;
        this.world_y = world_y;
        this.weapon = weapon;
        this.inventory = inventory;
        this.inventoryAmounts = inventoryAmounts;
        this.quest_progress = quest_progress;
        this.attack_experience = attack_experience;
        this.defence_experience = defence_experience;
        this.strength_experience = strength_experience;
        this.hitpoints_experience = hitpoints_experience;
        this.magic_experience = magic_experience;
        this.skin_color = skin_color;
        this.hair_color = hair_color;
        this.shirt_color = shirt_color;
        this.shield = shield;
        this.helmet = helmet;
        this.body_armor = body_armor;
        this.leg_armor = leg_armor;
        this.gloves = gloves;
        this.boots = boots;
        this.neckwear = neckwear;
        this.ring = ring;
        this.pants_color = pants_color;
    }

    public int getInventoryAmount(int index) {
        return inventoryAmounts[index];
    }

    public int getInventoryLength() {
        return inventory.length;
    }

    public int getPantsColor() {
        return pants_color;
    }

    public int getSkinColor() {
        return skin_color;
    }

    public int getHairColor() {
        return hair_color;
    }

    public int getShirtColor() {
        return shirt_color;
    }

    public int getPlayerID() {
        return player_id;
    }

    public int getAccountID() {
        return account_id;
    }

    public int getWorldX() {
        return world_x;
    }

    public int getWorldY() {
        return world_y;
    }

    public Integer getWeapon() {
        return weapon;
    }

    public int[] getInventory() {
        return inventory;
    }

    public int[] getInventoryAmounts() {
        return inventoryAmounts;
    }

    public int[] getQuestProgress() {
        return quest_progress;
    }

    public int getAttackExperience() {
        return attack_experience;
    }

    public int getDefenceExperience() {
        return defence_experience;
    }

    public int getStrengthExperience() {
        return strength_experience;
    }

    public int getHitpointsExperience() {
        return hitpoints_experience;
    }

    public Integer getShield() {
        return shield;
    }

    public Integer getHelmet() {
        return helmet;
    }

    public Integer getBodyArmor() {
        return body_armor;
    }

    public Integer getLegArmor() {
        return leg_armor;
    }

    public Integer getGloves() {
        return gloves;
    }

    public Integer getBoots() {
        return boots;
    }

    public Integer getNeckwear() {
        return neckwear;
    }

    public Integer getRing() {
        return ring;
    }

    public int getMagicExperience() {
        return magic_experience;
    }

}
