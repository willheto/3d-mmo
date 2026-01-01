package com.g8e.gameserver.models.entities;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.DropTable;
import com.g8e.gameserver.util.ExperienceUtils;
import com.g8e.gameserver.util.SkillUtils;
import com.g8e.util.Logger;

public class Npc extends Combatant {
    private int respawnTime;
    private int respawnTickCounter;
    public boolean isDead;

    public Npc(World world, int entityIndex, int worldX, int worldY, String name, String examine, int respawnTime,
            int[] skills,
            int type) {
        super("npc" + (int) (Math.random() * 1000000), entityIndex, world, worldX, worldY, name, examine, type);

        this.respawnTime = respawnTime;
        this.skills = skills;
        this.currentHitpoints = ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS]);
        this.weapon = null;
        this.combatLevel = getCombatLevel();

    }

    public void update() {
        this.updateCounters();

        if (interactionTargetID != null) {
            Entity entity = this.world.getEntityByID(interactionTargetID);
            if (entity == null || entity.isDying) {
                interactionTargetID = null;
                return;
            }

            int entityX = entity.worldX;
            int entityY = entity.worldY;

            int interactionRange = this.interactionRange;

            if (entityX < this.worldX - interactionRange || entityX > this.worldX + interactionRange
                    || entityY < this.worldY - interactionRange || entityY > this.worldY + interactionRange) {
                interactionTargetID = null;
                Logger.printError("Interaction target out of range");
                return;
            }

            // face the target
            if (entityX < this.worldX) {
                this.facingDirection = Direction.LEFT;
            } else if (entityX > this.worldX) {
                this.facingDirection = Direction.RIGHT;
            } else if (entityY < this.worldY) {
                this.facingDirection = Direction.UP;
            } else if (entityY > this.worldY) {
                this.facingDirection = Direction.DOWN;
            }

            this.targetTile = null;
            this.newTargetTile = null;
            this.nextTileDirection = null;
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
                this.setNewTargetTileWithingWanderArea();
            }
        }

        if (this.isTargetTileNotWithinWanderArea()) {
            this.targetedEntityID = null;
            this.setNewTargetTileWithingWanderArea();
        }

        if (this.targetedEntityID != null) {
            if (this.followCounter > 0) {
                this.followCounter--;
            } else {
                this.moveTowardsTarget();
            }
        } else {
            this.moveTowardsTarget();

        }

        if (this.targetedEntityID != null) {
            if (isOneStepAwayFromTarget()) {
                Entity entity = this.world.getEntityByID(((Combatant) this).targetedEntityID);
                if (entity != null && entity instanceof Combatant) {
                    ((Combatant) this).attackEntity((Combatant) entity, false);
                    // this.nextTileDirection = null;
                    this.targetTile = null;
                    this.newTargetTile = null;
                    this.targetEntityLastPosition = null;
                    int entityX = entity.worldX;
                    int entityY = entity.worldY;

                    this.facingDirection = this.getDirectionTowardsTile(entityX, entityY);
                    return;
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

        if ((Math.abs(this.worldX - target.worldX) == 1 && this.worldY == target.worldY) ||
                (Math.abs(this.worldY - target.worldY) == 1 && this.worldX == target.worldX)) {
            return true;
        }

        return false;
    }

    private void updateCounters() {
        if (this.snareCounter > 0) {
            this.snareCounter--;
        }
        if (this.attackTickCounter > 0) {
            this.attackTickCounter--;
        }

        if (this.lastDamageDealtCounter > 0) {
            this.lastDamageDealtCounter--;
        } else {
            this.lastDamageDealt = null;
        }

        if (this.isInCombatCounter > 0) {
            this.isInCombatCounter--;
        } else {
            this.isInCombat = false;
        }

        if (isDying) {
            this.dyingCounter++;
            if (this.dyingCounter > 5) {

                EntityData entityData = this.world.entitiesManager.getEntityDataByIndex(this.entityIndex);

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
                this.isDying = false;
                this.isDead = true;
                this.dyingCounter = 0;
            }
        }

    }

    public void resetNpc() {
        this.targetTile = null;
        this.newTargetTile = null;
        this.targetedEntityID = null;
        this.targetItemID = null;
        this.nextTileDirection = null;
        this.currentHitpoints = ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS]);
        this.worldX = this.originalWorldX;
        this.worldY = this.originalWorldY;
        this.tickCounter = 0;
        this.followCounter = 0;
        this.isInCombat = false;

    }

    public int getCombatLevel() {
        int hitpointsLevel = ExperienceUtils.getLevelByExp(skills[SkillUtils.HITPOINTS]);
        int attackLevel = ExperienceUtils.getLevelByExp(skills[SkillUtils.ATTACK]);
        int strengthLevel = ExperienceUtils.getLevelByExp(skills[SkillUtils.STRENGTH]);
        int defenceLevel = ExperienceUtils.getLevelByExp(skills[SkillUtils.DEFENCE]);

        double base = 0.25 * (defenceLevel + hitpointsLevel);
        double melee = 0.325 * (attackLevel + strengthLevel);

        return (int) (base + melee);
    }

}
