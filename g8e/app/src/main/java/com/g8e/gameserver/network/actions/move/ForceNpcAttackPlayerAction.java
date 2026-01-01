package com.g8e.gameserver.network.actions.move;

import com.g8e.gameserver.network.actions.Action;

public class ForceNpcAttackPlayerAction extends Action {
    private String npcID;

    public ForceNpcAttackPlayerAction(String playerID, String npcID) {
        this.action = "forceNpcAttackPlayer";
        this.playerID = playerID;
        this.npcID = npcID;
    }

    public String getPlayerID() {
        return playerID;
    }

    public String getNpcID() {
        return npcID;
    }
}
