package com.g8e.gameserver.models.events;

public class MagicEvent {
    public String entityID;
    public int spellID;
    public boolean isCastAnimation = false;

    public MagicEvent(String entityID, int spellID, boolean isCastAnimation) {
        this.entityID = entityID;
        this.spellID = spellID;
        this.isCastAnimation = isCastAnimation;
    }
}
