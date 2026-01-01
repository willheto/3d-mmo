package com.g8e.gameserver.models.entities;

import java.sql.SQLException;
import java.util.List;

import com.g8e.db.CommonQueries;
import com.g8e.db.models.DBPlayer;
import com.g8e.gameserver.World;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.Shop;
import com.g8e.gameserver.models.Stock;
import com.g8e.gameserver.models.events.MagicEvent;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.events.TradeEvent;
import com.g8e.gameserver.models.objects.Edible;
import com.g8e.gameserver.models.objects.Item;
import com.g8e.gameserver.models.objects.Wieldable;
import com.g8e.gameserver.models.quests.Quest;
import com.g8e.gameserver.models.quests.QuestReward;
import com.g8e.gameserver.models.spells.Spell;
import com.g8e.gameserver.network.actions.Action;
import com.g8e.gameserver.network.actions.ChangeAppearanceAction;
import com.g8e.gameserver.network.actions.attackStyle.ChangeAttackStyleAction;
import com.g8e.gameserver.network.actions.drop.DropItemAction;
import com.g8e.gameserver.network.actions.edibles.EatItemAction;
import com.g8e.gameserver.network.actions.inventory.AddItemToInventoryAction;
import com.g8e.gameserver.network.actions.inventory.RemoveItemFromInventoryAction;
import com.g8e.gameserver.network.actions.magic.CastSpellAction;
import com.g8e.gameserver.network.actions.move.ForceNpcAttackPlayerAction;
import com.g8e.gameserver.network.actions.move.PlayerAttackMove;
import com.g8e.gameserver.network.actions.move.PlayerMove;
import com.g8e.gameserver.network.actions.move.PlayerTakeMoveAction;
import com.g8e.gameserver.network.actions.move.PlayerTalkMoveAction;
import com.g8e.gameserver.network.actions.quest.QuestProgressUpdateAction;
import com.g8e.gameserver.network.actions.shop.BuyItemAction;
import com.g8e.gameserver.network.actions.shop.SellItemAction;
import com.g8e.gameserver.network.actions.shop.TradeMoveAction;
import com.g8e.gameserver.network.actions.use.UseItemAction;
import com.g8e.gameserver.network.actions.wield.UnwieldAction;
import com.g8e.gameserver.network.actions.wield.WieldItemAction;
import com.g8e.gameserver.tile.TilePosition;
import com.g8e.gameserver.util.ExperienceUtils;
import com.g8e.gameserver.util.SkillUtils;
import com.g8e.util.Logger;
import com.google.gson.Gson;

public class Player extends Combatant {

    private static final int playerStartingX = 0;
    private static final int playerStartingY = 0;
    public int accountID;
    public int[] inventory = new int[20];
    public int[] inventoryAmounts = new int[20];
    public int[] questProgress = new int[10];
    public int influence;
    public int skinColor;
    public int hairColor;
    public int shirtColor;
    public int pantsColor;
    public boolean needsFullChunkUpdate = false;
    public int teleportCounter = 0;
    public int spellCounter = 0;
    public String spellTarget = null;

    public Player(World world, DBPlayer dbPlayer, String uniquePlayerID, String username, int accountID) {
        super(uniquePlayerID, 0, world, dbPlayer.getWorldX(), dbPlayer.getWorldY(), username,
                "That's " + username + "!", 0);
        this.accountID = accountID;
        this.currentHitpoints = ExperienceUtils.getLevelByExp(dbPlayer.getHitpointsExperience());

        this.loadPlayerSkills(dbPlayer);
        this.combatLevel = this.getCombatLevel();
        this.weapon = dbPlayer.getWeapon();
        this.loadPlayerInventory(dbPlayer);
        this.loadQuestProgress(dbPlayer);
        this.attackStyle = "attack";
        this.skinColor = dbPlayer.getSkinColor();
        this.hairColor = dbPlayer.getHairColor();
        this.shirtColor = dbPlayer.getShirtColor();
        this.pantsColor = dbPlayer.getPantsColor();
        this.currentChunk = world.tileManager.getChunkByWorldXandY(dbPlayer.getWorldX(), dbPlayer.getWorldY());
        this.originalWorldX = playerStartingX;
        this.originalWorldY = playerStartingY;
    }

    @Override
    public int getCurrentChunk() {
        return this.currentChunk;
    }

    private void loadQuestProgress(DBPlayer dbPlayer) {
        for (int i = 0; i < dbPlayer.getQuestProgress().length; i++) {
            this.questProgress[i] = dbPlayer.getQuestProgress()[i];

            Quest quest = this.world.questsManager.getQuestByID(i);

            if (dbPlayer.getQuestProgress()[i] == 100 && quest != null) {
                influence += quest.getRewards().getInfluenceReward();
            }
        }

    }

    public void update() {
        this.updateCounters();

        if (spellCounter == 1) {
            if (spellUsed != null) {
                if (spellUsed.getSpellID() == 3) {
                    Entity target = this.world.getEntityByID(this.spellTarget);
                    if (target != null && target instanceof Combatant) {
                        ((Combatant) target).snareCounter = 10;
                        this.world.chatMessages
                                .add(new ChatMessage(target.name, "A magical force prevents you from moving!",
                                        System.currentTimeMillis(), false));
                        SoundEvent soundEvent = new SoundEvent("snare.wav", true, false, target.entityID, true);
                        this.world.tickMagicEvents.add(new MagicEvent(target.entityID, spellUsed.getSpellID(), false));
                        this.world.tickSoundEvents.add(soundEvent);
                    }
                }
                if (spellUsed.getSpellID() == 2) {
                    Entity target = this.world.getEntityByID(this.spellTarget);
                    if (target != null && target instanceof Combatant) {
                        SoundEvent soundEvent = new SoundEvent("magic_hit.wav", true, false, target.entityID, true);
                        this.world.tickMagicEvents.add(new MagicEvent(target.entityID, spellUsed.getSpellID(), false));
                        this.world.tickSoundEvents.add(soundEvent);
                        this.attackEntity((Combatant) target, true);
                    }
                }

                this.spellUsed = null;
                this.spellTarget = null;
            }
        }

        if (isDying) {
            return; // todo, some animation handling? maybe on client side though.
        }

        if (teleportCounter <= 5 && spellUsed != null && spellUsed.getType() == 1) {
            this.move(spellUsed.getTargetX(), spellUsed.getTargetY());

            this.needsFullChunkUpdate = true;
            this.spellUsed = null;
            SoundEvent soundEvent = new SoundEvent("teleport_arrive.wav", true, false, this.entityID, true);
            this.world.tickSoundEvents.add(soundEvent);
        }

        if (teleportCounter > 0) {
            return;
        }

        this.moveTowardsTarget();
        if (this.targetedEntityID != null) {
            if (goalAction == null) {
                Logger.printError("Goal action is null, but targeted entity is not null!");
                targetedEntityID = null;
                return;
            }

            if (goalAction == 2) {
                Entity target = this.world.getEntityByID(((Combatant) this).targetedEntityID);

                if (target.isDying == true) {
                    this.targetedEntityID = null;
                    this.goalAction = null;
                    this.newTargetTile = null;
                    this.targetTile = null;
                    this.targetEntityLastPosition = null;
                    return;
                }
            }

            if (isOneStepAwayFromTarget()) {
                if (goalAction == 2) {
                    Entity entity = this.world.getEntityByID(((Combatant) this).targetedEntityID);
                    if (entity != null && entity instanceof Combatant) {
                        ((Combatant) this).attackEntity((Combatant) entity, false);
                        this.nextTileDirection = null;
                        this.targetTile = null;
                        this.newTargetTile = null;
                        this.targetEntityLastPosition = null;
                        int entityX = entity.worldX;
                        int entityY = entity.worldY;

                        this.facingDirection = this.getDirectionTowardsTile(entityX, entityY);
                        return;
                    }
                } else if (goalAction == 1) {
                    Entity entity = this.world.getEntityByID(this.targetedEntityID);
                    if (entity != null && entity instanceof Npc) {
                        this.nextTileDirection = null;
                        this.targetTile = null;
                        this.newTargetTile = null;
                        this.targetEntityLastPosition = null;
                        this.goalAction = null;
                        this.targetedEntityID = null;
                        TalkEvent talkEvent = new TalkEvent(this.entityID, entity.entityID, entity.entityIndex);

                        if (entity instanceof Combatant && ((Combatant) entity).targetedEntityID == null) {
                            entity.interactionTargetID = this.entityID;
                        }

                        int entityX = entity.worldX;
                        int entityY = entity.worldY;

                        this.facingDirection = this.getDirectionTowardsTile(entityX, entityY);
                        this.world.tickTalkEvents.add(talkEvent);
                        return;
                    }

                } else if (goalAction == 3) {
                    Entity entity = this.world.getEntityByID(this.targetedEntityID);
                    if (entity != null && entity instanceof Npc) {
                        this.nextTileDirection = null;
                        this.targetTile = null;
                        this.newTargetTile = null;
                        this.targetEntityLastPosition = null;
                        this.goalAction = null;
                        this.targetedEntityID = null;
                        TradeEvent tradeEvent = new TradeEvent(this.entityID, entity.entityID, entity.entityIndex);
                        entity.interactionTargetID = this.entityID;
                        int entityX = entity.worldX;
                        int entityY = entity.worldY;

                        this.facingDirection = this.getDirectionTowardsTile(entityX, entityY);
                        this.world.tickTradeEvents.add(tradeEvent);
                        return;
                    }

                }
            }
        }
    }

    // TODO: replace magic numbers with maps
    public void runQuestScriptsForKill(int entityIndex) {
        if (entityIndex == 13) { // Killing the bandit chief
            this.questProgressUpdate(1, 4);
        }
    }

    private boolean isOneStepAwayFromTarget() {
        Entity target = this.world.getEntityByID(this.targetedEntityID);
        if (target == null) {
            Logger.printError("Target not found");
            return false;
        }

        if ((Math.abs(this.worldX - target.worldX) == 1 && this.worldY == target.worldY)
                || (Math.abs(this.worldY - target.worldY) == 1 && this.worldX == target.worldX)) {
            return true;
        }

        return false;
    }

    public void takeItem(String uniqueItemID) {
        Item item = this.world.itemsManager.getItemByUniqueItemID(uniqueItemID);
        if (item == null) {
            Logger.printError("Item not found");
            this.world.chatMessages
                    .add(new ChatMessage(this.name, "Too late, it's gone!", System.currentTimeMillis(), false));
            return;
        }
        addItemToInventory(item.getItemID(), item.getAmount());
        this.world.itemsManager.removeItem(item.getUniqueID());

        SoundEvent soundEvent = new SoundEvent("pick_up.wav", true, false, this.entityID, false);
        this.world.tickSoundEvents.add(soundEvent);

    }

    public void saveQuestProgress() {
        Gson gson = new Gson();
        String questProgressString = gson.toJson(this.questProgress);
        try {
            CommonQueries.savePlayerQuestProgressByAccountId(this.accountID, questProgressString);
        } catch (Exception e) {
            Logger.printError("Failed to save quest progress");
        }
    }

    public void savePosition() {
        try {
            CommonQueries.savePlayerPositionByAccountId(this.accountID, this.worldX, this.worldY);
        } catch (Exception e) {
            Logger.printError("Failed to save player position");
        }
    }

    public void saveInventory() {
        Gson gson = new Gson();
        String inventoryString = gson.toJson(this.inventory);
        String inventoryAmountsString = gson.toJson(this.inventoryAmounts);
        try {
            CommonQueries.savePlayerInventoryByAccountId(this.accountID, inventoryString, inventoryAmountsString);
        } catch (Exception e) {
            Logger.printError("Failed to save inventory");
        }
    }

    public void addXp(int skill, int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP cannot be negative");
        }

        if (xp == 0) {
            return;
        }

        int previousLevel = ExperienceUtils.getLevelByExp(this.skills[skill]);
        this.skills[skill] += xp;
        int currentLevel = ExperienceUtils.getLevelByExp(this.skills[skill]);

        if (this.skills[skill] > 200_000_000) {
            this.skills[skill] = 200_000_000;
        }

        if (currentLevel > previousLevel) {
            String levelUpMessage = "Congratulations, your " + SkillUtils.getSkillNameByNumber(skill) + " level is now "
                    + currentLevel + ".";

            long timeSent = System.currentTimeMillis();
            ChatMessage chatMessageModel = new ChatMessage(this.name, levelUpMessage, timeSent, false);
            this.world.chatMessages.add(chatMessageModel);

            if (skill == SkillUtils.ATTACK) {
                SoundEvent soundEvent = new SoundEvent("attack_level_up.ogg", true, true, this.entityID, true);
                this.world.tickSoundEvents.add(soundEvent);
            } else if (skill == SkillUtils.STRENGTH) {
                SoundEvent soundEvent = new SoundEvent("strength_level_up.ogg", true, true, this.entityID, false);
                this.world.tickSoundEvents.add(soundEvent);
            } else if (skill == SkillUtils.DEFENCE) {
                SoundEvent soundEvent = new SoundEvent("defence_level_up.ogg", true, true, this.entityID, false);
                this.world.tickSoundEvents.add(soundEvent);
            } else if (skill == SkillUtils.MAGIC) {
                SoundEvent soundEvent = new SoundEvent("magic_level_up.ogg", true, true, this.entityID, false);
                this.world.tickSoundEvents.add(soundEvent);
            } else if (skill == SkillUtils.HITPOINTS) {
                SoundEvent soundEvent = new SoundEvent("hitpoints_level_up.ogg", true, true, this.entityID, false);
                this.world.tickSoundEvents.add(soundEvent);
            }

        }

        this.combatLevel = this.getCombatLevel();
        saveSkillXp(skill);
    }

    public void saveSkillXp(int skill) {
        try {
            CommonQueries.savePlayerXpByAccountId(this.accountID, skill, this.skills[skill]);
        } catch (Exception e) {
            Logger.printError("Failed to save skill xp");
        }
    }

    private void updateInventoryOrder(int[] requestedInventory) {
        if (requestedInventory.length != this.inventory.length) {
            Logger.printError("Inventory length mismatch");
            return;
        }

        // check if inventory has same items but in different order
        for (int inventory : this.inventory) {
            boolean found = false;
            for (int requestedItem : requestedInventory) {
                if (inventory == requestedItem) {
                    continue;
                }
            }

            if (!found) {
                Logger.printError("Inventory mismatch");
                return;
            }
        }

        this.inventory = requestedInventory;
    }

    public void setTickActions(List<Action> actions) {
        for (Action action : actions) {

            if (action instanceof ChangeAppearanceAction) {
                ChangeAppearanceAction changeAppearanceAction = (ChangeAppearanceAction) action;
                this.skinColor = changeAppearanceAction.getSkinColor();
                this.hairColor = changeAppearanceAction.getHairColor();
                this.shirtColor = changeAppearanceAction.getShirtColor();
                this.pantsColor = changeAppearanceAction.getPantsColor();
                try {
                    CommonQueries.savePlayerAppearanceByAccountId(
                            this.accountID, this.skinColor, this.hairColor, this.shirtColor, this.pantsColor);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }

            if (action instanceof PlayerMove) {
                PlayerMove playerMove = (PlayerMove) action;
                this.newTargetTile = new TilePosition(playerMove.getX(), playerMove.getY());
                this.targetItemID = null;
                this.targetedEntityID = null;
                this.goalAction = null;
            }

            if (action instanceof PlayerAttackMove) {
                PlayerAttackMove playerAttackMove = (PlayerAttackMove) action;
                Entity npc = this.world.getEntityByID(playerAttackMove.getEntityID());
                if (npc == null) {
                    Logger.printError("NPC not found");
                    return;
                }
                Entity entity = this.world.getEntityByID(playerAttackMove.getEntityID());

                this.newTargetTile = new TilePosition(npc.worldX, npc.worldY);
                this.targetedEntityID = playerAttackMove.getEntityID();
                this.goalAction = 2; // Attack action TODO
            }

            if (action instanceof DropItemAction) {
                DropItemAction dropItemAction = (DropItemAction) action;
                this.dropItem(dropItemAction.getInventoryIndex());
            }

            if (action instanceof WieldItemAction) {
                WieldItemAction wieldItemAction = (WieldItemAction) action;

                int itemID = this.inventory[wieldItemAction.getInventoryIndex()];
                Wieldable item = this.world.itemsManager.getWieldableInfoByItemID(itemID);

                if (item == null) {
                    Logger.printError("Item not found or not wieldable");
                    return;
                }

                if (item.getType().equals("sword") || item.getType().equals("axe")) {
                    this.weapon = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("shield")) {
                    this.shield = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("helmet")) {
                    this.helmet = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("bodyArmor")) {
                    this.bodyArmor = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("legArmor")) {
                    this.legArmor = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("gloves")) {
                    this.gloves = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("boots")) {
                    this.boots = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("neckwear")) {
                    this.neckwear = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else if (item.getType().equals("ring")) {
                    this.ring = wieldItemAction.getInventoryIndex();
                    saveWieldables();
                } else {
                    Logger.printError("Item is not wieldable");
                }

            }

            if (action instanceof PlayerTakeMoveAction) {
                PlayerTakeMoveAction playerTakeMoveAction = (PlayerTakeMoveAction) action;
                this.handlePlayerTakeMove(playerTakeMoveAction.getUniqueItemID());
                this.goalAction = null;
            }

            if (action instanceof UseItemAction) {
                UseItemAction useItemAction = (UseItemAction) action;
                this.useItem(useItemAction.getItemID(), useItemAction.getTargetID());
            }

            if (action instanceof UnwieldAction) {
                UnwieldAction unwieldAction = (UnwieldAction) action;
                this.unwieldItem(unwieldAction.getInventoryIndex());
            }

            if (action instanceof EatItemAction) {
                EatItemAction eatItemAction = (EatItemAction) action;
                this.eatItem(eatItemAction.getInventoryIndex());
            }

            if (action instanceof QuestProgressUpdateAction) {
                QuestProgressUpdateAction questProgressUpdateAction = (QuestProgressUpdateAction) action;
                this.questProgressUpdate(questProgressUpdateAction.getQuestID(),
                        questProgressUpdateAction.getProgress());
            }

            if (action instanceof PlayerTalkMoveAction) {
                PlayerTalkMoveAction playerTalkMoveAction = (PlayerTalkMoveAction) action;

                Entity entity = this.world.getEntityByID(playerTalkMoveAction.getEntityID());
                if (entity != null) {
                    this.targetedEntityID = playerTalkMoveAction.getEntityID();
                    this.goalAction = 1;
                    this.newTargetTile = new TilePosition(entity.worldX, entity.worldY);
                }
            }

            if (action instanceof ChangeAttackStyleAction) {
                ChangeAttackStyleAction changeAttackStyleAction = (ChangeAttackStyleAction) action;
                this.attackStyle = changeAttackStyleAction.getAttackStyle();
            }

            if (action instanceof RemoveItemFromInventoryAction) {
                RemoveItemFromInventoryAction removeItemFromInventoryAction = (RemoveItemFromInventoryAction) action;
                int itemID = removeItemFromInventoryAction.getItemID();
                if (removeItemFromInventoryAction.getAmount() == 0) {
                    for (int i = 0; i < this.inventory.length; i++) {
                        if (this.inventory[i] == itemID) {
                            this.inventory[i] = 0;
                            this.inventoryAmounts[i] = 0;
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < this.inventory.length; i++) {
                        if (this.inventory[i] == itemID) {
                            this.inventoryAmounts[i] -= removeItemFromInventoryAction.getAmount();
                            if (this.inventoryAmounts[i] <= 0) {
                                this.inventory[i] = 0;
                                this.inventoryAmounts[i] = 0;
                            }
                            break;
                        }
                    }
                }
                saveInventory();

            }

            if (action instanceof AddItemToInventoryAction) {
                AddItemToInventoryAction addItemToInventoryAction = (AddItemToInventoryAction) action;
                int itemID = addItemToInventoryAction.getItemID();
                int quantity = addItemToInventoryAction.getQuantity();
                this.addItemToInventory(itemID, quantity);
            }

            if (action instanceof ForceNpcAttackPlayerAction) {
                ForceNpcAttackPlayerAction forceNpcAttackPlayerAction = (ForceNpcAttackPlayerAction) action;
                Entity entity = this.world.getEntityByID(forceNpcAttackPlayerAction.getNpcID());
                if (entity != null && entity instanceof Npc) {
                    ((Npc) entity).targetedEntityID = this.entityID;
                }
            }

            if (action instanceof BuyItemAction) {
                BuyItemAction buyItemAction = (BuyItemAction) action;
                handleBuyItemAction(buyItemAction.getShopID(), buyItemAction.getItemID(),
                        buyItemAction.getAmount());
            }

            if (action instanceof SellItemAction) {
                SellItemAction sellItemAction = (SellItemAction) action;
                handleSellItemAction(sellItemAction.getShopID(), sellItemAction.getInventoryIndex(),
                        sellItemAction.getAmount());
            }

            if (action instanceof TradeMoveAction) {
                TradeMoveAction tradeMoveAction = (TradeMoveAction) action;
                Entity entity = this.world.getEntityByID(tradeMoveAction.getEntityID());
                if (entity != null && entity instanceof Player) {
                    // this.tradeWithPlayer((Player) entity); // TODO
                } else {
                    this.targetedEntityID = tradeMoveAction.getEntityID();
                    this.goalAction = 3;
                    this.newTargetTile = new TilePosition(entity.worldX, entity.worldY);
                }
            }

            if (action instanceof CastSpellAction) {
                CastSpellAction castSpellAction = (CastSpellAction) action;
                this.castSpell(castSpellAction.getSpellID(), castSpellAction.getTargetID());
            }
        }
    }

    private void castSpell(int spellID, String targetID) {
        if (spellCounter > 0) {
            this.world.chatMessages.add(
                    new ChatMessage(this.name, "You're already casting a spell!", System.currentTimeMillis(), false));
            return;
        }
        Spell spell = world.spellsManager.getSpellByID(spellID);

        if (spell == null) {
            Logger.printError("Spell not found");
            return;
        }

        int levelRequirement = spell.getLevelRequirement();
        int playerMagicLevel = ExperienceUtils.getLevelByExp(this.skills[SkillUtils.MAGIC]);

        if (playerMagicLevel < levelRequirement) {
            this.world.chatMessages.add(
                    new ChatMessage(this.name, "You need a magic level of " + levelRequirement + " to cast this spell.",
                            System.currentTimeMillis(), false));
            return;
        }

        if (spell.getType() == 1) { // Teleport spell
            if (spell.getTargetX() == -1 || spell.getTargetY() == -1) {
                Logger.printError("Spell target not set");
                return;
            }

            if (teleportCounter > 0) {
                this.world.chatMessages.add(new ChatMessage(this.name, "You are already teleporting!",
                        System.currentTimeMillis(), false));
                return;
            }
            // move to next
            this.clearTarget();

            if (this.nextTileDirection != null) {
                moveToNextTile();
                this.nextTileDirection = null;
                this.currentPath = null;
            }

            SoundEvent soundEvent = new SoundEvent("teleport.wav", true, false, this.entityID, true);
            this.world.tickSoundEvents.add(soundEvent);
            this.teleportCounter = 10;
            this.spellUsed = spell;
            this.world.tickMagicEvents.add(new MagicEvent(entityID, spellID, true));

            // check if anyone is targeting this player
            for (Player player : world.players) {
                if (player.targetedEntityID != null && player.targetedEntityID.equals(this.entityID)) {
                    player.clearTarget();
                }
            }

            for (Npc npc : world.npcs) {
                if (npc.targetedEntityID != null && npc.targetedEntityID.equals(this.entityID)) {
                    npc.clearTarget();
                }
            }
        } else {
            if (targetID == null) {
                Logger.printError("Target not set");
                return;
            }

            if (targetID.equals(this.entityID)) {
                Logger.printError("Cannot cast spell on self");
                this.world.chatMessages.add(new ChatMessage(this.name, "You cannot cast this spell on yourself.",
                        System.currentTimeMillis(), false));
                return;
            }

            Entity target = this.world.getEntityByID(targetID);

            if (target == null) {
                Logger.printError("Target not found");
                return;
            }

            if (target.type == 1) {
                this.world.chatMessages.add(new ChatMessage(this.name, "You wouldn't want to do that.",
                        System.currentTimeMillis(), false));
                return;
            }

            if (teleportCounter > 0) {
                this.world.chatMessages.add(new ChatMessage(this.name, "Cannot cast while teleporting!",
                        System.currentTimeMillis(), false));
                return;
            }

            // face the target
            this.facingDirection = this.getDirectionTowardsTile(target.worldX, target.worldY);

            this.clearTarget();

            if (this.nextTileDirection != null) {
                moveToNextTile();
                this.nextTileDirection = null;
                this.currentPath = null;
            }

            if (spell.getSpellID() == 3) { // snare
                if (target instanceof Combatant) {
                    int distance = Math.abs(this.worldX - target.worldX) + Math.abs(this.worldY - target.worldY);

                    if (distance > 7) {
                        this.world.chatMessages.add(new ChatMessage(this.name, "The target is too far away.",
                                System.currentTimeMillis(), false));
                        return;
                    }

                    this.spellUsed = spell;
                    this.spellCounter = 4;
                    this.spellTarget = targetID;
                    this.world.tickMagicEvents.add(new MagicEvent(this.entityID, spellID, true));
                }
            }

            if (spell.getSpellID() == 2) {
                if (target instanceof Combatant) {
                    int distance = Math.abs(this.worldX - target.worldX) + Math.abs(this.worldY - target.worldY);

                    if (distance > 7) {
                        this.world.chatMessages.add(new ChatMessage(this.name, "The target is too far away.",
                                System.currentTimeMillis(), false));
                        return;
                    }

                    this.spellUsed = spell;
                    this.spellCounter = 4;
                    this.spellTarget = targetID;
                    this.world.tickMagicEvents.add(new MagicEvent(this.entityID, spellID, true));
                    SoundEvent soundEvent = new SoundEvent("magic_cast.wav", true, false, target.entityID, true);
                    this.world.tickSoundEvents.add(soundEvent);

                }
            }
        }
    }

    private void handleSellItemAction(String shopID, int inventoryIndex, int amount) {
        // Validate input
        if (amount <= 0) {
            Logger.printError("Invalid sell quantity.");
            return;
        }

        Shop shop = world.shopsManager.getShopByID(shopID);
        if (shop == null) {
            Logger.printError("Shop not found");
            return;
        }

        int itemID = this.inventory[inventoryIndex];
        Item item = world.itemsManager.getItemByID(itemID);
        if (itemID == 0 || item == null) {
            Logger.printError("Item not found in inventory");
            return;
        }

        Stock stock = shop.getStock(itemID);

        if (shop.getBuysAnything() == false) {
            if (stock == null) {
                world.chatMessages.add(new ChatMessage(this.name, "The shop is not interested in that item.",
                        System.currentTimeMillis(), false));
                return;
            }
        }

        // Check how many items the player has
        int playerItemQuantity = 0;
        if (item.isStackable()) {
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    playerItemQuantity += this.inventoryAmounts[i];
                }
            }
        } else {
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    playerItemQuantity++;
                }
            }
        }

        // If the player does not have enough items, sell all items they have
        if (playerItemQuantity == 0) {
            world.addChatMessage(new ChatMessage(this.name, "You don't have any of that item to sell.",
                    System.currentTimeMillis(), false));
            return;
        }

        // Set amount to sell to the available quantity, if amount is more than the
        // player has
        if (amount > playerItemQuantity) {
            amount = playerItemQuantity;
        }

        // Calculate the total sell price
        int sellPrice = (int) Math.floor(item.getValue() * shop.getBuysAtPercentage());
        long totalSellPrice = (long) sellPrice * amount;
        if (totalSellPrice > Integer.MAX_VALUE) {
            Logger.printError("Total price exceeds the maximum value.");
            return;
        }

        // Deduct items from the player's inventory
        int remainingAmount = amount;
        if (item.isStackable()) {
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    if (this.inventoryAmounts[i] >= remainingAmount) {
                        this.inventoryAmounts[i] -= remainingAmount;
                        remainingAmount = 0;
                        if (this.inventoryAmounts[i] == 0) {
                            this.inventory[i] = 0;
                        }
                        break;
                    } else {
                        remainingAmount -= this.inventoryAmounts[i];
                        this.inventoryAmounts[i] = 0;
                        this.inventory[i] = 0;
                    }
                }
            }
        } else {
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    this.inventory[i] = 0;
                    remainingAmount--;
                    if (remainingAmount == 0) {
                        break;
                    }
                }
            }
        }

        if (remainingAmount > 0) {
            Logger.printError("Error while deducting items from inventory.");
            return;
        }

        // Add coins to the player
        addCoins((int) totalSellPrice);

        // Update the shop's stock
        // if shop already has item on stock-> add amount to stock
        if (stock != null) {
            if (stock.getQuantity() + amount > Integer.MAX_VALUE) {
                world.addChatMessage(new ChatMessage(this.name,
                        "The shop cannot accept more of this item.", System.currentTimeMillis(), false));
                return;
            }

            stock.setQuantity(stock.getQuantity() + amount);
        } else {
            // if shop does not have item on stock -> create new stock
            // -1 means no restocking
            Stock newStock = new Stock(itemID, amount, -1);
            newStock.setIsDefaultStock(false);
            shop.addStock(newStock);
        }

        // Save changes to the inventory
        saveInventory();
    }

    private void addCoins(int totalSellPrice) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 102) {
                if ((long) this.inventoryAmounts[i] + totalSellPrice > Integer.MAX_VALUE) {
                    world.chatMessages.add(new ChatMessage(this.name,
                            "You already have a full stack of coins.",
                            System.currentTimeMillis(), false));
                    return;
                }
                this.inventoryAmounts[i] += totalSellPrice;
                saveInventory();
                return;
            }
        }

        int emptySlot = getEmptyInventorySlot();
        if (emptySlot == -1) {
            world.chatMessages.add(new ChatMessage(this.name,
                    "You don't have enough space in your inventory. The coins are dropped on the ground.",
                    System.currentTimeMillis(), false));
            world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, 102, 200, totalSellPrice);
            return;
        }

        this.inventory[emptySlot] = 102;
        this.inventoryAmounts[emptySlot] = totalSellPrice;
        saveInventory();
    }

    private void handleBuyItemAction(String shopID, int itemID, int amount) {
        if (amount <= 0) {
            Logger.printError("Invalid purchase quantity.");
            return;
        }

        Shop shop = world.shopsManager.getShopByID(shopID);

        if (shop == null) {
            Logger.printError("Shop not found");
            return;
        }

        Stock stock = shop.getStock(itemID);
        if (stock == null) {
            Logger.printError("Item not found in shop");
            return;
        }

        int playerCoins = 0;

        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 102) {
                playerCoins += this.inventoryAmounts[i];
            }
        }

        Item item = world.itemsManager.getItemByID(itemID);
        if (item == null) {
            Logger.printError("Item not found in items manager, buy action failed.");
            this.world.addChatMessage(new ChatMessage(this.name, "Item not found", System.currentTimeMillis(), false));
            return;
        }

        int availableAmount = amount;
        if (stock.getQuantity() < amount) {
            if (stock.getQuantity() == 0) {
                world.addChatMessage(new ChatMessage(this.name, "The shop is out of stock.", System.currentTimeMillis(),
                        false));
                return;
            }
            availableAmount = stock.getQuantity();
        }

        int totalPrice = (int) Math.floor(item.getValue() * shop.getSellsAtPercentage() * availableAmount);

        if (totalPrice > Integer.MAX_VALUE) {
            Logger.printError("Total price exceeds the maximum value.");
            return;
        }

        if (playerCoins < totalPrice) {
            world.addChatMessage(new ChatMessage(this.name, "You don't have enough coins.", System.currentTimeMillis(),
                    false));
            return;
        }

        if (world.itemsManager.getItemByID(itemID).isStackable() == true) {
            boolean isItemAlreadyInInventory = false;
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    if ((long) this.inventoryAmounts[i] + availableAmount > Integer.MAX_VALUE) {
                        Logger.printError("Quantity exceeds maximum limit for item stack.");
                        world.chatMessages.add(new ChatMessage(this.name,
                                "You already have a full stack of this item.",
                                System.currentTimeMillis(), false));

                        return;
                    }
                    this.inventoryAmounts[i] += availableAmount;

                    isItemAlreadyInInventory = true;
                    break;
                }
            }

            if (isItemAlreadyInInventory == false) {
                int emptySlot = getEmptyInventorySlot();

                if (emptySlot == -1) {
                    world.chatMessages.add(new ChatMessage(this.name,
                            "You don't have enough space in your inventory.",
                            System.currentTimeMillis(), false));
                    return;
                }

                this.inventory[emptySlot] = itemID;
                this.inventoryAmounts[emptySlot] = availableAmount;
            }

        } else {
            for (int i = 0; i < availableAmount; i++) {
                int emptySlot = getEmptyInventorySlot();
                if (emptySlot == -1) {
                    world.addChatMessage(new ChatMessage(this.name, "You don't have enough space in your inventory.",
                            System.currentTimeMillis(), false));
                    return;
                }

                this.inventory[emptySlot] = itemID;
            }
        }

        saveInventory();
        stock.setQuantity(stock.getQuantity() - availableAmount);
        if (stock.getQuantity() == 0 && stock.isDefaultStock() == false) {
            shop.removeStock(itemID);
        }
        this.removeCoins(totalPrice);
    }

    private void removeCoins(int amount) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 102) {
                if (this.inventoryAmounts[i] >= amount) {
                    this.inventoryAmounts[i] -= amount;
                    saveInventory();
                    return;
                } else {
                    world.addChatMessage(new ChatMessage(this.name, "You don't have enough coins.",
                            System.currentTimeMillis(), false));
                }
            }
        }
    }

    private void addItemToInventory(int itemID, int quantity) {
        Item item = this.world.itemsManager.getItemByID(itemID);
        if (item == null) {
            Logger.printError("Item not found");
            return;
        }

        if (item.isStackable() == true) {
            boolean isItemAlreadyInInventory = false;
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    if ((long) this.inventoryAmounts[i] + quantity > Integer.MAX_VALUE) {
                        Logger.printError("Quantity exceeds maximum limit for item stack.");
                        world.chatMessages.add(new ChatMessage(this.name,
                                "You already have a full stack of this item. The item is dropped on the ground.",
                                System.currentTimeMillis(), false));

                        world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, itemID, 200, quantity);
                        return;
                    }
                    this.inventoryAmounts[i] += quantity;
                    isItemAlreadyInInventory = true;
                    break;
                }
            }

            if (isItemAlreadyInInventory == false) {
                int emptySlot = getEmptyInventorySlot();

                if (emptySlot == -1) {
                    Logger.printError("No empty inventory slots, dropping item");
                    world.chatMessages.add(new ChatMessage(this.name,
                            "You don't have enough space in your inventory. The item is dropped on the ground.",
                            System.currentTimeMillis(), false));
                    world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, itemID, 200, quantity);
                    return;
                }

                this.inventory[emptySlot] = itemID;
                this.inventoryAmounts[emptySlot] = quantity;
            }
        } else {
            if (!item.isStackable() && quantity > 1) {
                Logger.printError("Cannot add multiple non-stackable items.");
                return;
            }
            int emptySlot = getEmptyInventorySlot();

            if (emptySlot == -1) {
                world.chatMessages.add(new ChatMessage(this.name,
                        "You don't have enough space in your inventory. The item is dropped on the ground.",
                        System.currentTimeMillis(), false));
                world.itemsManager.spawnItem(this.worldX, this.worldY, itemID, 200);
                return;
            }

            this.inventory[emptySlot] = itemID;
        }

        saveInventory();
    }

    private void eatItem(int inventoryIndex) {

        if (inventoryIndex < 0 || inventoryIndex >= this.inventory.length) {
            Logger.printError("Invalid inventory index");
            return;
        }

        int itemID = this.inventory[inventoryIndex];
        Item item = this.world.itemsManager.getItemByID(itemID);
        Edible edible = this.world.itemsManager.getEdibleInfoByItemID(itemID);

        if (item == null || edible == null) {
            Logger.printError("Item not found or not edible");
            return;
        }

        this.inventory[inventoryIndex] = 0;
        if (item.isStackable()) {
            this.inventoryAmounts[inventoryIndex] = 0;
        }
        this.currentHitpoints += edible.getHealAmount();
        this.world.chatMessages
                .add(new ChatMessage(this.name, "You eat the " + item.getName() + ". " + "It heals some health.",
                        System.currentTimeMillis(),
                        false));

        if (this.currentHitpoints > ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS])) {
            this.currentHitpoints = ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS]);
        }

        SoundEvent soundEvent = new SoundEvent("eat.wav", true, false, this.entityID, false);
        this.world.tickSoundEvents.add(soundEvent);
        this.attackTickCounter = 4;

    }

    // TODO add item use functionality
    private void useItem(int itemID, int targetID) {
        this.world.chatMessages
                .add(new ChatMessage(this.name, "Nothing interesting happens.", System.currentTimeMillis(), false));

    }

    private void handlePlayerTakeMove(String uniqueItemID) {
        Item item = this.world.itemsManager.getItemByUniqueItemID(uniqueItemID);
        if (item == null) {
            this.world.chatMessages
                    .add(new ChatMessage(this.name, "Too late, it's gone!", System.currentTimeMillis(), false));
            return;
        }

        if (item.getWorldX() == this.worldX && item.getWorldY() == this.worldY) {
            this.takeItem(uniqueItemID);
            return;
        }

        this.targetItemID = uniqueItemID;
        this.newTargetTile = new TilePosition(item.getWorldX(), item.getWorldY());
    }

    private void questProgressUpdate(int questID, int progress) {
        if (questID < 0 || questID >= this.questProgress.length) {
            Logger.printError("Invalid quest ID");
            return;
        }

        this.questProgress[questID] = progress;
        saveQuestProgress();

        if (progress == 100) { // 100 is the completion value
            ChatMessage chatMessage = new ChatMessage(this.name, "Congratulations, you've completed a quest!",
                    System.currentTimeMillis(), false);

            this.world.chatMessages.add(chatMessage);
            SoundEvent soundEvent = new SoundEvent("quest_complete.ogg", true, true, this.entityID, false);
            this.world.tickSoundEvents.add(soundEvent);
            Quest quest = this.world.questsManager.getQuestByID(questID);
            QuestReward reward = quest.getRewards();
            influence += reward.getInfluenceReward();
            int[] skillRewards = reward.getSkillRewards();

            for (int i = 0; i < skillRewards.length; i++) {
                this.addXp(i, skillRewards[i]);
            }

            int[] itemRewards = reward.getItemRewards();
            for (int itemID : itemRewards) {
                int emptySlot = getEmptyInventorySlot();

                if (emptySlot == -1) {
                    Logger.printError("No empty inventory slots, dropping item");
                    this.world.itemsManager.spawnItem(this.worldX, this.worldY, itemID);
                    return;
                }

                Item item = this.world.itemsManager.getItemByID(itemID);

                if (item == null) {
                    Logger.printError("Item not found");
                    return;
                }

                this.inventory[emptySlot] = itemID;
                if (item.isStackable()) {
                    this.inventoryAmounts[emptySlot] = item.getAmount();
                }

                saveInventory();

            }

        }
    }

    private int getEmptyInventorySlot() {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 0) {
                return i;
            }
        }

        return -1;
    }

    public void saveWieldables() {
        try {
            CommonQueries.savePlayerWieldablesByAccountId(this.accountID, this.weapon, this.shield, this.helmet,
                    this.bodyArmor, this.legArmor, this.gloves, this.boots, this.neckwear, this.ring);
        } catch (Exception e) {
            Logger.printError("Failed to save weapon");
        }
    }

    private void dropItem(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex >= this.inventory.length) {
            Logger.printError("Invalid inventory index");
            return;
        }

        int itemID = this.inventory[inventoryIndex];
        if (itemID == 0) {
            Logger.printError("No item to drop");
            return;
        }

        if (this.weapon != null && this.weapon == inventoryIndex) {
            this.weapon = null;
            saveWieldables();
        }

        Item item = this.world.itemsManager.getItemByID(itemID);

        if (item == null) {
            Logger.printError("Item not found");
            return;
        }

        this.inventory[inventoryIndex] = 0;
        if (item.isStackable()) {
            int amountToDrop = this.inventoryAmounts[inventoryIndex];
            this.world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, itemID, 200, amountToDrop);
            this.inventoryAmounts[inventoryIndex] = 0;
        } else {
            this.world.itemsManager.spawnItem(this.worldX, this.worldY, itemID, 200);
        }

        SoundEvent soundEvent = new SoundEvent("drop.wav", true, false, this.entityID, false);
        this.world.tickSoundEvents.add(soundEvent);
        saveInventory();
    }

    private void unwieldItem(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex >= this.inventory.length) {
            Logger.printError("Invalid inventory index");
            return;
        }

        int itemID = this.inventory[inventoryIndex];
        Item item = this.world.itemsManager.getItemByID(itemID);

        if (item == null) {
            Logger.printError("Item not found");
            return;
        }

        if (item.isWieldable() == false) {
            Logger.printError("Item is not wieldable");
            return;
        }

        if (this.weapon == inventoryIndex) {
            this.weapon = null;

        } else if (this.shield != null && this.shield == inventoryIndex) {
            this.shield = null;

        } else if (this.helmet != null && this.helmet == inventoryIndex) {
            this.helmet = null;

        } else if (this.bodyArmor != null && this.bodyArmor == inventoryIndex) {
            this.bodyArmor = null;

        } else if (this.legArmor != null && this.legArmor == inventoryIndex) {
            this.legArmor = null;

        } else if (this.gloves != null && this.gloves == inventoryIndex) {
            this.gloves = null;

        } else if (this.boots != null && this.boots == inventoryIndex) {
            this.boots = null;

        } else if (this.neckwear != null && this.neckwear == inventoryIndex) {
            this.neckwear = null;

        } else if (this.ring != null && this.ring == inventoryIndex) {
            this.ring = null;

        } else {
            Logger.printError("Item not wielded");
            return;
        }

        saveWieldables();

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

    private void updateCounters() {
        if (this.snareCounter > 0) {
            this.snareCounter--;
        }
        if (this.spellCounter > 0) {
            this.spellCounter--;
        }

        if (this.teleportCounter > 0) {
            this.teleportCounter--;
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
                this.resetPlayer();
                this.isDying = false;
                this.dyingCounter = 0;
            }
        }

    }

    private void loadPlayerSkills(DBPlayer player) {
        this.skills[SkillUtils.ATTACK] = player.getAttackExperience();
        this.skills[SkillUtils.STRENGTH] = player.getStrengthExperience();
        this.skills[SkillUtils.DEFENCE] = player.getDefenceExperience();
        this.skills[SkillUtils.HITPOINTS] = player.getHitpointsExperience();
        this.skills[SkillUtils.MAGIC] = player.getMagicExperience();
    }

    private void loadPlayerInventory(DBPlayer player) {
        this.inventory = new int[20];
        this.inventoryAmounts = new int[20];
        for (int i = 0; i < player.getInventory().length; i++) {
            this.inventory[i] = player.getInventory()[i];
        }

        for (int i = 0; i < player.getInventoryAmounts().length; i++) {
            this.inventoryAmounts[i] = player.getInventoryAmounts()[i];
        }
    }

    public void killPlayer() {
        this.isDying = true;
        this.nextTileDirection = null;
        this.world.chatMessages
                .add(new ChatMessage(this.name, "Oh dear, you are dead!", System.currentTimeMillis(), false));
    }

    public void resetPlayer() {
        this.weapon = null;
        this.helmet = null;
        this.shield = null;
        this.bodyArmor = null;
        this.legArmor = null;
        this.gloves = null;
        this.boots = null;
        this.neckwear = null;
        this.ring = null;
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != 0) {
                int amount = this.inventoryAmounts[i];
                int itemID = this.inventory[i];

                if (amount > 0) {
                    this.world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, itemID, 200, amount);
                } else {
                    this.world.itemsManager.spawnItem(this.worldX, this.worldY, itemID, 200);
                }
            }
        }
        this.inventory = new int[20];
        this.inventoryAmounts = new int[20];
        this.currentHitpoints = ExperienceUtils.getLevelByExp(this.skills[3]);
        move(this.originalWorldX, this.originalWorldY);
        this.targetTile = null;
        this.newTargetTile = null;
        this.targetedEntityID = null;
        this.targetItemID = null;
        this.isInCombatCounter = 0;
        this.lastDamageDealt = null;
        this.lastDamageDealtCounter = 0;
        this.attackTickCounter = 0;
        this.currentPath = null;
        this.nextTileDirection = null;
        this.goalAction = null;
        this.isInCombat = false;
        this.saveInventory();
        this.saveWieldables();
    }

}
