package com.g8e.gameserver.network.actions.wield;

public class UnwieldActionData {
    final private int inventoryIndex;

    public UnwieldActionData(int inventoryIndex) {
        this.inventoryIndex = inventoryIndex;
    }

    public int getInventoryIndex() {
        return inventoryIndex;
    }

}
