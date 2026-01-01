package com.g8e.gameserver.network.dataTransferModels;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.Chunkable;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.spells.Spell;
import com.g8e.gameserver.pathfinding.PathNode;

public class DTOPlayer implements Chunkable {
    // Player fields
    public int[] inventory;
    public int[] inventoryAmounts;
    public int[] questProgress;
    public int influence;
    public int skinColor;
    public int hairColor;
    public int shirtColor;
    public int pantsColor;
    public int teleportCounter;
    public int spellCounter;
    public String spellTarget;
    public Spell spellUsed;

    // Combatant fields
    public int[] skills;
    public int currentHitpoints;
    public boolean isInCombat;

    public int combatLevel;
    public Integer weapon = null;
    public Integer helmet = null;
    public Integer shield = null;
    public Integer bodyArmor = null;
    public Integer legArmor = null;
    public Integer gloves = null;
    public Integer boots = null;
    public Integer neckwear = null;
    public Integer ring = null;

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
    public String name;
    public int currentChunk;
    public List<PathNode> currentPath;

    public DTOPlayer(Player player) {
        this.inventory = player.inventory;
        this.inventoryAmounts = player.inventoryAmounts;
        this.questProgress = player.questProgress;
        this.influence = player.influence;
        this.skinColor = player.skinColor;
        this.hairColor = player.hairColor;
        this.shirtColor = player.shirtColor;
        this.pantsColor = player.pantsColor;
        this.teleportCounter = player.teleportCounter;
        this.spellCounter = player.spellCounter;
        this.spellTarget = player.spellTarget;
        this.spellUsed = player.spellUsed;

        this.skills = player.skills;
        this.currentHitpoints = player.currentHitpoints;
        this.isInCombat = player.isInCombat;

        this.combatLevel = player.combatLevel;
        this.weapon = player.weapon;
        this.helmet = player.helmet;
        this.shield = player.shield;
        this.bodyArmor = player.bodyArmor;
        this.legArmor = player.legArmor;
        this.gloves = player.gloves;
        this.boots = player.boots;
        this.neckwear = player.neckwear;
        this.ring = player.ring;
        this.attackStyle = player.attackStyle;
        this.isDying = player.isDying;
        this.lastDamageDealt = player.lastDamageDealt;

        this.entityID = player.entityID;
        this.entityIndex = player.entityIndex;
        this.worldX = player.worldX;
        this.worldY = player.worldY;
        this.nextTileDirection = player.nextTileDirection;
        this.facingDirection = player.facingDirection;
        this.name = player.name;
        this.currentChunk = player.currentChunk;
        this.currentPath = player.currentPath;

    }

    @Override
    public int getCurrentChunk() {
        return this.currentChunk;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        DTOPlayer other = (DTOPlayer) obj;

        return Arrays.hashCode(this.inventory) == Arrays.hashCode(other.inventory) &&
                Arrays.hashCode(this.inventoryAmounts) == Arrays.hashCode(other.inventoryAmounts) &&
                Arrays.hashCode(this.questProgress) == Arrays.hashCode(other.questProgress) &&
                Arrays.hashCode(this.skills) == Arrays.hashCode(other.skills) &&
                this.influence == other.influence &&
                this.skinColor == other.skinColor &&
                this.hairColor == other.hairColor &&
                this.shirtColor == other.shirtColor &&
                this.pantsColor == other.pantsColor &&
                this.currentHitpoints == other.currentHitpoints &&
                this.isInCombat == other.isInCombat &&
                this.combatLevel == other.combatLevel &&
                Objects.equals(this.weapon, other.weapon) &&
                Objects.equals(this.helmet, other.helmet) &&
                Objects.equals(this.shield, other.shield) &&
                Objects.equals(this.bodyArmor, other.bodyArmor) &&
                Objects.equals(this.legArmor, other.legArmor) &&
                Objects.equals(this.gloves, other.gloves) &&
                Objects.equals(this.boots, other.boots) &&
                Objects.equals(this.neckwear, other.neckwear) &&
                Objects.equals(this.ring, other.ring) &&
                Objects.equals(this.attackStyle, other.attackStyle) &&
                this.isDying == other.isDying &&
                Objects.equals(this.entityID, other.entityID) &&
                this.entityIndex == other.entityIndex &&
                this.worldX == other.worldX &&
                this.worldY == other.worldY &&
                Objects.equals(this.nextTileDirection, other.nextTileDirection) &&
                Objects.equals(this.facingDirection, other.facingDirection) &&
                Objects.equals(this.name, other.name) &&
                this.currentChunk == other.currentChunk &&
                this.teleportCounter == other.teleportCounter &&
                this.spellCounter == other.spellCounter &&
                Objects.equals(this.spellTarget, other.spellTarget) &&
                Objects.equals(this.spellUsed, other.spellUsed);
    }

    public String getEntityID() {
        return entityID;
    }

}
