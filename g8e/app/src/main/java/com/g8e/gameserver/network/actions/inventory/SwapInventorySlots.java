package com.g8e.gameserver.network.actions.inventory;

import com.g8e.gameserver.network.actions.Action;

public class SwapInventorySlots extends Action {

    private final int fromSlot;
    private final int toSlot;

    public SwapInventorySlots(String playerID, int fromSlot, int toSlot) {
        this.action = "swapInventorySlots";
        this.playerID = playerID;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
    }

    public int getToSlot() {
        return toSlot;
    }

    public int getFromSlot() {
        return fromSlot;
    }
}
