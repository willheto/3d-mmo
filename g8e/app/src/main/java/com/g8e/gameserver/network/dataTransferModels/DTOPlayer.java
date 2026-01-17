package com.g8e.gameserver.network.dataTransferModels;

import java.util.Arrays;
import java.util.List;

import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.pathfinding.PathNode;

public class DTOPlayer {
    // Player fields
    public int[] inventory;
    public int[] inventoryAmounts;
    public int[] questProgress;
    public Integer influence;
    public Integer skinColor;
    public Integer hairColor;
    public Integer shirtColor;
    public Integer pantsColor;
    public String username;

    // Combatant fields
    public int[] skills;
    public Integer currentHitpoints;
    public Boolean isInCombat;
    public Integer weapon;
    public Integer shield;
    public String attackStyle;
    public Integer lastDamageDealt;

    // Entity fields
    public String entityID;
    public Integer worldX;
    public Integer worldY;
    public Integer lastTickX;
    public Integer lastTickY;
    public Direction facingDirection;
    public List<PathNode> currentPath;
    public Boolean isDying;

    public DTOPlayer(Player player) {
        this.entityID = player.entityID;

        if (player.inventoryChanged == 1) {
            this.inventory = Arrays.copyOf(player.inventory, player.inventory.length);
        }
        if (player.inventoryAmountsChanged == 1) {
            this.inventoryAmounts = Arrays.copyOf(player.inventoryAmounts, player.inventoryAmounts.length);
        }
        if (player.questProgressChanged == 1) {
            this.questProgress = Arrays.copyOf(player.questProgress, player.questProgress.length);
        }
        if (player.skillsChanged == 1) {
            this.skills = Arrays.copyOf(player.skills, player.skills.length);
        }
        if (player.influenceChanged == 1) {
            this.influence = player.influence;
        }

        if (player.skinColorChanged == 1)
            this.skinColor = player.skinColor;
        if (player.hairColorChanged == 1)
            this.hairColor = player.hairColor;
        if (player.shirtColorChanged == 1)
            this.shirtColor = player.shirtColor;
        if (player.pantsColorChanged == 1)
            this.pantsColor = player.pantsColor;

        if (player.currentHitpointsChanged == 1) {
            this.currentHitpoints = player.currentHitpoints;
        }

        if (player.isInCombatChanged == 1) {
            this.isInCombat = player.isInCombat;
        }

        if (player.weaponChanged == 1)
            this.weapon = player.weapon;
        if (player.shieldChanged == 1)
            this.shield = player.shield;

        if (player.attackStyleChanged == 1) {
            this.attackStyle = player.attackStyle;
        }

        if (player.isDyingChanged == 1) {
            this.isDying = player.isDying;
        }

        if (player.lastDamageDealtChanged == 1) {
            this.lastDamageDealt = player.lastDamageDealt;
        }

        if (player.worldXChanged == 1)
            this.worldX = player.worldX;
        if (player.worldYChanged == 1)
            this.worldY = player.worldY;

        if (player.facingDirectionChanged == 1) {
            this.facingDirection = player.facingDirection;
        }

        if (player.usernameChanged == 1) {
            this.username = player.username;
        }

        if (player.worldXChanged == 1 || player.worldYChanged == 1) {
            this.lastTickX = player.lastTickX;
            this.lastTickY = player.lastTickY;
        }

    }

    public DTOPlayer(Player player, boolean includeEverything) {
        this.entityID = player.entityID;
        this.inventory = Arrays.copyOf(player.inventory, player.inventory.length);
        this.inventoryAmounts = Arrays.copyOf(player.inventoryAmounts, player.inventoryAmounts.length);
        this.questProgress = Arrays.copyOf(player.questProgress, player.questProgress.length);
        this.skills = Arrays.copyOf(player.skills, player.skills.length);
        this.influence = player.influence;
        this.skinColor = player.skinColor;
        this.hairColor = player.hairColor;
        this.shirtColor = player.shirtColor;
        this.pantsColor = player.pantsColor;
        this.currentHitpoints = player.currentHitpoints;
        this.isInCombat = player.isInCombat;
        this.weapon = player.weapon;
        this.shield = player.shield;
        this.attackStyle = player.attackStyle;
        this.isDying = player.isDying;
        this.lastDamageDealt = player.lastDamageDealt;
        this.worldX = player.worldX;
        this.worldY = player.worldY;
        this.facingDirection = player.facingDirection;
        this.username = player.username;

    }

    public boolean hasOnlyEntityId() {
        return entityID != null
                && inventory == null
                && inventoryAmounts == null
                && questProgress == null
                && skills == null
                && influence == null
                && skinColor == null
                && hairColor == null
                && shirtColor == null
                && pantsColor == null
                && username == null
                && currentHitpoints == null
                && isInCombat == null
                && weapon == null
                && shield == null
                && attackStyle == null
                && lastDamageDealt == null
                && worldX == null
                && worldY == null
                && facingDirection == null
                && currentPath == null
                && isDying == null
                && lastTickX == null
                && lastTickY == null;
    }

    public String getEntityID() {
        return entityID;
    }

}
