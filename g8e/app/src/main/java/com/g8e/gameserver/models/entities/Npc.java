package com.g8e.gameserver.models.entities;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.DropTable;
import com.g8e.gameserver.util.ExperienceUtils;
import com.g8e.gameserver.util.SkillUtils;
import com.g8e.util.Logger;

public class Npc extends Combatant {
    public int npcIndex;

    private transient final int respawnTime;
    private transient int respawnTickCounter;
    public transient boolean isDead;
    public transient EntityData entityStaticData;

    public transient int npcIndexChanged = 1;
    public transient int respawnTickCounterChanged = 1;
    public transient int isDeadChanged = 1;
    public transient int entityStaticDataChanged = 1;

    public Npc(World world, int npcIndex, int worldX, int worldY) {
        super("npc" + (int) (Math.random() * 1000000), world, worldX, worldY);

        this.entityStaticData = this.world.entitiesManager.getEntityDataByIndex(npcIndex);
        if (entityStaticData == null) {
            throw new Error("Static entity data not found");
        }

        this.respawnTime = entityStaticData.respawnTime;
        this.skills = entityStaticData.skills;

        this.npcIndex = npcIndex;
        this.currentHitpoints = ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS]);
    }

    public void clearChangedFlags() {
        npcIndexChanged = 0;
        respawnTickCounterChanged = 0;
        isDeadChanged = 0;
        entityStaticDataChanged = 0;

        skillsChanged = 0;
        currentHitpointsChanged = 0;
        targetedEntityIDChanged = 0;

        lastDamageDealtChanged = 0;
        lastDamageDealtCounterChanged = 0;
        attackTickCounterChanged = 0;
        isInCombatCounterChanged = 0;
        attackStyleChanged = 0;

        weaponChanged = 0;
        shieldChanged = 0;
        isInCombatChanged = 0;

        worldXChanged = 0;
        worldYChanged = 0;
        facingDirectionChanged = 0;

        targetTileChanged = 0;
        newTargetTileChanged = 0;
        nextTileDirectionChanged = 0;
        currentPathChanged = 0;
        targetEntityLastPositionChanged = 0;

        followCounterChanged = 0;
        shouldFollowChanged = 0;
        dyingCounterChanged = 0;

        targetItemIDChanged = 0;
        interactionTargetIDChanged = 0;

        goalActionChanged = 0;
        wanderRangeChanged = 0;
        interactionRangeChanged = 0;

        isDyingChanged = 0;
        lastDamageDealtChanged = 0;
        entityIDChanged = 0;
    }

    @Override
    public void update() {
        updateCounters();

        if (interactionTargetID != null) {
            Entity entity = this.world.getEntityByID(interactionTargetID);
            if (entity == null || entity.isDying) {
                setInteractionTargetID(null);
                return;
            }

            int entityX = entity.worldX;
            int entityY = entity.worldY;

            if (entityX < this.worldX - interactionRange || entityX > this.worldX + interactionRange
                    || entityY < this.worldY - interactionRange || entityY > this.worldY + interactionRange) {
                setInteractionTargetID(null);
                return;
            }

            // face the target
            if (entityX < this.worldX) {
                setFacingDirection(Direction.LEFT);
            } else if (entityX > this.worldX) {
                setFacingDirection(Direction.RIGHT);
            } else if (entityY < this.worldY) {
                setFacingDirection(Direction.UP);
            } else if (entityY > this.worldY) {
                setFacingDirection(Direction.DOWN);
            }

            stopAllMovement();
            return;
        }

        if (isDying) {
            return;
        }

        if (isDead) {
            if (this.respawnTickCounter < this.respawnTime) {
                this.respawnTickCounter++;
            } else {
                this.respawnTickCounter = 0;
                this.isDead = false;
            }
        }

        if (this.targetedEntityID == null) {
            // 20% chance to set new target
            if (Math.random() < 0.05) {
                setNewTargetTileWithingWanderArea();
            }
        }

        if (this.targetTile != null && this.isTargetTileNotWithinWanderArea()) {
            setTargetedEntityID(null);
            setNewTargetTileWithingWanderArea();
        }

        if (this.targetedEntityID != null) {
            if (this.followCounter > 0) {
                setFollowCounter(--this.followCounter);
            } else {
                moveTowardsTarget();
            }
        } else {
            moveTowardsTarget();
        }

        if (this.targetedEntityID != null) {
            if (isOneStepAwayFromTarget()) {
                Entity entity = this.world.getEntityByID(((Combatant) this).targetedEntityID);
                if (entity != null && entity instanceof Combatant) {
                    ((Combatant) this).attackEntity((Combatant) entity);
                    stopAllMovement();
                    setFacingDirection(this.getDirectionTowardsTile(entity.worldX, entity.worldY));
                }
            }
        }
    }

    private boolean isOneStepAwayFromTarget() {
        Entity target = this.world.getEntityByID(this.targetedEntityID);
        if (target == null) {
            Logger.printError("Target not found");
            return false;
        }

        return (Math.abs(this.worldX - target.worldX) == 1 && this.worldY == target.worldY) ||
                (Math.abs(this.worldY - target.worldY) == 1 && this.worldX == target.worldX);
    }

    private void updateCounters() {

        if (this.attackTickCounter > 0) {
            setAttackTickCounter(--attackTickCounter);
        }

        if (this.lastDamageDealtCounter > 0) {
            setLastDamageDealtCounter(--lastDamageDealtCounter);
        } else if (lastDamageDealt != -1) {
            setLastDamageDealt(-1);
        }

        if (this.isInCombatCounter > 0) {
            setIsInCombatCounter(--isInCombatCounter);
        } else if (isInCombat != false) {
            setIsInCombat(false);
        }

        if (isDying) {
            this.dyingCounter++;
            if (this.dyingCounter > 5) {
                EntityData entityData = this.world.entitiesManager.getEntityDataByIndex(npcIndex);

                if (entityData.dropTable != null) {
                    // select random item from drop table
                    DropTable firstDrop = DropTable.getRolledDrop(entityData.dropTable);
                    DropTable secondDrop = DropTable.getRolledDrop(entityData.dropTable);

                    if (firstDrop != null) {
                        if (firstDrop.getAmount() > 0) {
                            this.world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, firstDrop.getItemID(),
                                    200, firstDrop.getAmount());
                        } else {
                            this.world.itemsManager.spawnItem(this.worldX, this.worldY, firstDrop.getItemID(), 200);
                        }
                    }
                    if (secondDrop != null) {
                        if (secondDrop.getAmount() > 0) {
                            this.world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY,
                                    secondDrop.getItemID(),
                                    200, secondDrop.getAmount());
                        } else {
                            this.world.itemsManager.spawnItem(this.worldX, this.worldY, secondDrop.getItemID(), 200);
                        }
                    }
                }

                this.resetNpc();
                setIsDying(false);
                this.isDead = true;
                this.dyingCounter = 0;
            }
        }

    }

    public void resetNpc() {
        setCurrentHitpoints(ExperienceUtils.getLevelByExp(this.skills[3]));
        move(this.originalWorldX, this.originalWorldY);
        setTargetTile(null);
        setNewTargetTile(null);
        setTargetedEntityID(null);
        setTargetItemID(null);
        setIsInCombatCounter(0);
        setLastDamageDealt(-1);
        setLastDamageDealtCounter(0);
        setAttackTickCounter(0);
        setCurrentPath(null);
        setNextTileDirection(Direction.NONE);
        setGoalAction(null);
        setIsInCombat(false);
    }

}
