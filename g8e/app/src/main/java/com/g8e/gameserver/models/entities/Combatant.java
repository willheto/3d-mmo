package com.g8e.gameserver.models.entities;

import java.util.concurrent.TimeUnit;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.events.AttackEvent;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.objects.Wieldable;
import com.g8e.gameserver.util.CombatUtils;
import com.g8e.gameserver.util.ExperienceUtils;
import com.g8e.gameserver.util.SkillUtils;
import com.g8e.util.Logger;

public abstract class Combatant extends Entity {
    public int[] skills = new int[5];
    public int currentHitpoints;
    public int weapon = -1;
    public int shield = -1;
    public int lastDamageDealt = -1;
    public String attackStyle;
    public boolean isInCombat;

    public transient String targetedEntityID = null;
    public transient int lastDamageDealtCounter;
    public transient int attackTickCounter;
    public transient int isInCombatCounter;

    public transient int skillsChanged = 1;
    public transient int currentHitpointsChanged = 1;
    public transient int targetedEntityIDChanged = 1;
    public transient int lastDamageDealtChanged = 1;
    public transient int lastDamageDealtCounterChanged = 1;
    public transient int attackTickCounterChanged = 1;
    public transient int isInCombatCounterChanged = 1;
    public transient int attackStyleChanged = 1;
    public transient int weaponChanged = 1;
    public transient int shieldChanged = 1;
    public transient int isInCombatChanged = 1;

    public Combatant(String entityID, World world, int worldX, int worldY) {
        super(entityID, world, worldX, worldY);
    }

    public void attackEntity(Combatant entity) {
        if (this.entityID.equals(entity.entityID)) {
            Logger.printError("Player cannot attack itself");
            return;
        }

        if (this.attackTickCounter != 0) {
            return;
        }

        if (entity.isDying) {
            return;
        }

        if (entity.worldX < this.worldX) {
            setFacingDirection(Direction.LEFT);
        } else if (entity.worldX > this.worldX) {
            setFacingDirection(Direction.RIGHT);
        } else if (entity.worldY < this.worldY) {
            setFacingDirection(Direction.UP);
        } else if (entity.worldY > this.worldY) {
            setFacingDirection(Direction.DOWN);
        }

        Wieldable weaponData = null;
        if (this instanceof Player && this.weapon != -1) {
            weaponData = this.world.itemsManager.getWieldableInfoByItemID(((Player) this).inventory[this.weapon]);
        }

        setAttackTickCounter(weaponData != null ? weaponData.getAttackSpeed() : 4);

        int accuracyBonus = weaponData != null ? weaponData.getAccuracyBonus() : 0;
        int strengthBonus = weaponData != null ? weaponData.getStrengthBonus() : 0;

        int attackDamage = CombatUtils.getAttackDamage(this.skills[SkillUtils.ATTACK], this.skills[SkillUtils.STRENGTH],
                entity.skills[SkillUtils.DEFENCE],
                accuracyBonus,
                strengthBonus,
                0);

        entity.setCurrentHitpoints(entity.currentHitpoints - attackDamage);
        if (entity instanceof Player && attackDamage > 0) {
            SoundEvent soundEvent = new SoundEvent("player_hit.ogg", true, false, this.entityID, true);
            this.world.tickSoundEvents.add(soundEvent);
        }
        if (entity.currentHitpoints < 0) {
            entity.setCurrentHitpoints(0);
        }
        int multiplier = 1;

        if (this instanceof Player player) {
            switch (this.attackStyle) {
                case "attack" -> player.addXp(SkillUtils.ATTACK, (4 * attackDamage) * multiplier);
                case "strength" -> player.addXp(SkillUtils.STRENGTH, (4 * attackDamage) * multiplier);
                case "defence" -> player.addXp(SkillUtils.DEFENCE, (4 * attackDamage) * multiplier);
                default -> {
                }
            }

            player.addXp(SkillUtils.HITPOINTS, (1 * attackDamage) * multiplier);
        }

        entity.setLastDamageDealt(attackDamage);
        entity.setLastDamageDealtCounter(1);
        entity.setIsInCombatCounter(20);
        entity.setIsInCombat(true);

        AttackEvent attackEvent = new AttackEvent(this.entityID,
                entity.entityID);

        if (this.weapon == -1) {
            SoundEvent soundEvent = new SoundEvent("punch.ogg", true, false, this.entityID, true);
            this.world.tickSoundEvents.add(soundEvent);
        } else {
            SoundEvent soundEvent = new SoundEvent("sword_slash.ogg", true, false, this.entityID, true);
            this.world.tickSoundEvents.add(soundEvent);
        }
        this.world.tickAttackEvents.add(attackEvent);

        if (entity.currentHitpoints <= 0) {
            this.clearTarget();
            switch (entity) {
                case Npc npc -> {
                    npc.setIsDying(true);
                    SoundEvent soundEvent = new SoundEvent("man_death.ogg", true, false, this.entityID, true);
                    this.world.tickSoundEvents.add(soundEvent);
                    npc.setNextTileDirection(Direction.NONE);
                }
                case Player player -> {
                    player.killPlayer();
                    SoundEvent soundEvent = new SoundEvent("man_death.ogg", true, false, this.entityID, true);
                    this.world.tickSoundEvents.add(soundEvent);
                    SoundEvent soundEvent2 = new SoundEvent("death.ogg", true, true, entity.entityID, false);
                    this.world.tickSoundEvents.add(soundEvent2);
                }
                default -> {
                }
            }

        }

        setFollowCounter(2);

        if (entity instanceof Npc) {
            if (entity.targetedEntityID == null) {
                this.world.scheduler.schedule(() -> {
                    entity.clearTarget();
                    entity.setInteractionTargetID(null);
                    entity.setTargetItemID(null);
                    entity.setTargetedEntityID(this.entityID);
                }, 400, TimeUnit.MILLISECONDS);
            }
        }

    }

    protected void clearTarget() {
        setTargetedEntityID(null);
        setTargetTile(null);
        setNewTargetTile(null);
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

    protected void setTargetedEntityID(String id) {
        this.targetedEntityID = id;
        this.targetedEntityIDChanged = 1;
    }

    public void setSkills(int[] skills) {
        this.skills = skills;
        this.skillsChanged = 1;
    }

    public void setCurrentHitpoints(int currentHitpoints) {
        this.currentHitpoints = currentHitpoints;
        this.currentHitpointsChanged = 1;
    }

    public void setLastDamageDealt(int lastDamageDealt) {
        this.lastDamageDealt = lastDamageDealt;
        this.lastDamageDealtChanged = 1;
    }

    public void setLastDamageDealtCounter(int lastDamageDealtCounter) {
        this.lastDamageDealtCounter = lastDamageDealtCounter;
        this.lastDamageDealtCounterChanged = 1;
    }

    public void setAttackTickCounter(int attackTickCounter) {
        this.attackTickCounter = attackTickCounter;
        this.attackTickCounterChanged = 1;
    }

    public void setIsInCombatCounter(int isInCombatCounter) {
        this.isInCombatCounter = isInCombatCounter;
        this.isInCombatCounterChanged = 1;
    }

    public void setAttackStyle(String attackStyle) {
        this.attackStyle = attackStyle;
        this.attackStyleChanged = 1;
    }

    public void setWeapon(Integer weapon) {
        this.weapon = weapon;
        this.weaponChanged = 1;
    }

    public void setShield(Integer shield) {
        this.shield = shield;
        this.shieldChanged = 1;
    }

    public void setIsInCombat(boolean isInCombat) {
        this.isInCombat = isInCombat;
        this.isInCombatChanged = 1;
    }

}
