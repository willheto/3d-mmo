package com.g8e.gameserver.models.objects;

public class Item {
    private String uniqueID;
    private int itemID;
    private int amount;
    public Integer worldX = null;
    public Integer worldY = null;

    private String name;
    private String examine;
    private boolean isWieldable;
    private String spriteName;
    final private int value;
    private boolean isStackable;
    private String type;
    public boolean isDeleted;

    public int uniqueIDChanged = 1;
    public int itemIDChanged = 1;
    public int amountChanged = 1;
    public int worldXChanged = 1;
    public int worldYChanged = 1;

    public Item(int itemID, String name, String examine, boolean isWieldable, boolean isStackable, String spriteName,
            int value) {
        this.itemID = itemID;
        this.name = name;
        this.examine = examine;
        this.isWieldable = isWieldable;
        this.isStackable = isStackable;
        this.spriteName = spriteName;
        this.value = value;
        this.amount = 1;
    }

    public void clearChangedFlags() {
        uniqueIDChanged = 0;
        itemIDChanged = 0;
        amountChanged = 0;
        worldXChanged = 0;
        worldYChanged = 0;
    }

    public int getValue() {
        return value;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        this.amountChanged = 1;
    }

    public boolean isStackable() {
        return isStackable;
    }

    public void setStackable(boolean stackable) {
        isStackable = stackable;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExamine() {
        return examine;
    }

    public void setExamine(String examine) {
        this.examine = examine;
    }

    public boolean isWieldable() {
        return isWieldable;
    }

    public void setWieldable(boolean wieldable) {
        isWieldable = wieldable;
    }

    public String getSpriteName() {
        return spriteName;
    }

    public void setSpriteName(String spriteName) {
        this.spriteName = spriteName;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
        this.uniqueIDChanged = 1;
    }

    public Integer getWorldX() {
        return worldX;
    }

    public void setWorldX(Integer worldX) {
        this.worldX = worldX;
        this.worldXChanged = 1;
    }

    public Integer getWorldY() {
        return worldY;
    }

    public void setWorldY(Integer worldY) {
        this.worldY = worldY;
        this.worldYChanged = 1;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = true;
    }
}
