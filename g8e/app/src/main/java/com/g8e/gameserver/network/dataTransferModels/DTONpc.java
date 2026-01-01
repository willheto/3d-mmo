package com.g8e.gameserver.network.dataTransferModels;

import org.apache.commons.lang3.builder.EqualsBuilder;

import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.Chunkable;
import com.g8e.gameserver.models.entities.Npc;

public class DTONpc implements Chunkable {
    // Npc fields
    public boolean isDead;

    // Combatant fields
    public int[] skills;
    public int currentHitpoints;
    public boolean isInCombat;
    public int combatLevel;
    public Integer weapon = null;
    public String attackStyle;
    public boolean isDying = false;
    public Integer lastDamageDealt = null;

    // Entity fields
    public String entityID;
    public int entityIndex;
    public int worldX;
    public int worldY;
    public Direction nextTileDirection = null;
    public Direction facingDirection = Direction.DOWN;
    public int currentChunk;

    public DTONpc(Npc npc) {
        this.isDead = npc.isDead;

        this.skills = npc.skills;
        this.currentHitpoints = npc.currentHitpoints;
        this.isInCombat = npc.isInCombat;

        this.combatLevel = npc.combatLevel;
        this.weapon = npc.weapon;
        this.attackStyle = npc.attackStyle;
        this.isDying = npc.isDying;
        this.lastDamageDealt = npc.lastDamageDealt;

        this.entityID = npc.entityID;
        this.entityIndex = npc.entityIndex;
        this.worldX = npc.worldX;
        this.worldY = npc.worldY;
        this.nextTileDirection = npc.nextTileDirection;
        this.facingDirection = npc.facingDirection;
        this.currentChunk = npc.currentChunk;
    }

    public String getEntityID() {
        return this.entityID;
    }

    @Override
    public int getCurrentChunk() {
        return this.currentChunk;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DTONpc other = (DTONpc) obj;
        return new EqualsBuilder()
                .append(this.isDead, other.isDead)
                .append(this.skills, other.skills)
                .append(this.currentHitpoints, other.currentHitpoints)
                .append(this.isInCombat, other.isInCombat)
                .append(this.combatLevel, other.combatLevel)
                .append(this.weapon, other.weapon)
                .append(this.attackStyle, other.attackStyle)
                .append(this.isDying, other.isDying)
                .append(this.entityID, other.entityID)
                .append(this.entityIndex, other.entityIndex)
                .append(this.worldX, other.worldX)
                .append(this.worldY, other.worldY)
                .append(this.nextTileDirection, other.nextTileDirection)
                .append(this.facingDirection, other.facingDirection)
                .append(this.currentChunk, other.currentChunk)
                .isEquals();

    }

}
