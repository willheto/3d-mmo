package com.g8e.gameserver.network.dataTransferModels;

import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.entities.Npc;

public class DTONpc {
    // Npc fields
    public Integer npcIndex;

    // Combatant fields
    public Integer currentHitpoints;
    public Boolean isInCombat;
    public Integer lastDamageDealt;

    // Entity fields
    public String entityID;
    public Integer worldX;
    public Integer worldY;
    public Integer lastTickX;
    public Integer lastTickY;
    public Direction nextTileDirection;
    public Direction facingDirection;
    public Boolean isDying;

    public DTONpc(Npc npc) {
        this.entityID = npc.entityID;

        if (npc.npcIndexChanged == 1) {
            this.npcIndex = npc.npcIndex;
        }

        if (npc.currentHitpointsChanged == 1) {
            this.currentHitpoints = npc.currentHitpoints;
        }

        if (npc.isInCombatChanged == 1) {
            this.isInCombat = npc.isInCombat;
        }

        if (npc.isDyingChanged == 1) {

            this.isDying = npc.isDying;
        }

        if (npc.lastDamageDealtChanged == 1) {
            this.lastDamageDealt = npc.lastDamageDealt;
        }

        if (npc.worldXChanged == 1) {
            this.worldX = npc.worldX;
        }

        if (npc.worldYChanged == 1) {
            this.worldY = npc.worldY;
        }

        if (npc.facingDirectionChanged == 1) {
            this.facingDirection = npc.facingDirection;
        }

        if (npc.worldXChanged == 1 || npc.worldYChanged == 1) {
            this.lastTickX = npc.lastTickX;
            this.lastTickY = npc.lastTickY;
        }
    }

    public DTONpc(Npc npc, boolean includeEverything) {
        this.entityID = npc.entityID;
        this.npcIndex = npc.npcIndex;
        this.currentHitpoints = npc.currentHitpoints;
        this.isInCombat = npc.isInCombat;
        this.isDying = npc.isDying;
        this.lastDamageDealt = npc.lastDamageDealt;
        this.worldX = npc.worldX;
        this.worldY = npc.worldY;
        this.facingDirection = npc.facingDirection;
    }

    public boolean hasOnlyEntityId() {
        return entityID != null
                && npcIndex == null
                && currentHitpoints == null
                && isInCombat == null
                && lastDamageDealt == null
                && worldX == null
                && worldY == null
                && nextTileDirection == null
                && facingDirection == null
                && isDying == null
                && lastTickX == null
                && lastTickY == null;
    }

    public String getEntityID() {
        return this.entityID;
    }

}
