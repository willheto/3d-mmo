package com.g8e.gameserver.models.entities;

import java.sql.SQLException;
import java.util.List;

import com.g8e.db.CommonQueries;
import com.g8e.db.models.DBPlayer;
import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.enums.GoalAction;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.events.TradeEvent;
import com.g8e.gameserver.models.objects.Edible;
import com.g8e.gameserver.models.objects.Item;
import com.g8e.gameserver.models.objects.Wieldable;
import com.g8e.gameserver.models.quests.Quest;
import com.g8e.gameserver.models.quests.QuestReward;
import com.g8e.gameserver.network.actions.Action;
import com.g8e.gameserver.network.actions.ChangeAppearanceAction;
import com.g8e.gameserver.network.actions.attackStyle.ChangeAttackStyleAction;
import com.g8e.gameserver.network.actions.drop.DropItemAction;
import com.g8e.gameserver.network.actions.edibles.EatItemAction;
import com.g8e.gameserver.network.actions.inventory.AddItemToInventoryAction;
import com.g8e.gameserver.network.actions.inventory.RemoveItemFromInventoryAction;
import com.g8e.gameserver.network.actions.inventory.SwapInventorySlots;
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
    public int[] inventory = new int[12];
    public int[] inventoryAmounts = new int[12];
    public int[] questProgress = new int[10];
    public int influence;
    public int skinColor;
    public int hairColor;
    public int shirtColor;
    public int pantsColor;

    final public String username;

    private transient static final int PLAYER_STARTING_X = 0;
    private transient static final int PLAYER_STARTING_Y = 0;
    public transient int accountID;

    public transient int inventoryChanged = 1;
    public transient int inventoryAmountsChanged = 1;
    public transient int questProgressChanged = 1;
    public transient int influenceChanged = 1;
    public transient int skinColorChanged = 1;
    public transient int hairColorChanged = 1;
    public transient int shirtColorChanged = 1;
    public transient int pantsColorChanged = 1;
    public transient int usernameChanged = 1;

    public Player(World world, DBPlayer dbPlayer, String uniquePlayerID, String username, int accountID) {
        super(uniquePlayerID, world, dbPlayer.getWorldX(), dbPlayer.getWorldY());
        this.accountID = accountID;
        this.username = username;

        this.originalWorldX = PLAYER_STARTING_X;
        this.originalWorldY = PLAYER_STARTING_Y;

        this.loadPlayerSkills(dbPlayer);
        this.loadPlayerInventory(dbPlayer);
        this.loadQuestProgress(dbPlayer);

        this.weapon = dbPlayer.getWeapon();
        this.shield = dbPlayer.getShield();
        this.skinColor = dbPlayer.getSkinColor();
        this.hairColor = dbPlayer.getHairColor();
        this.shirtColor = dbPlayer.getShirtColor();
        this.pantsColor = dbPlayer.getPantsColor();
        this.currentHitpoints = ExperienceUtils.getLevelByExp(dbPlayer.getHitpointsExperience());

        this.attackStyle = "attack";
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

    public void clearChangedFlags() {
        skinColorChanged = 0;
        hairColorChanged = 0;
        shirtColorChanged = 0;
        pantsColorChanged = 0;
        usernameChanged = 0;

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
        influenceChanged = 0;
        lastDamageDealtChanged = 0;
        entityIDChanged = 0;

        questProgressChanged = 0;
        inventoryAmountsChanged = 0;
        inventoryChanged = 0;
        skillsChanged = 0;
    }

    @Override
    public void update() {
        this.updateCounters();

        if (isDying) {
            return;
        }

        this.moveTowardsTarget();
        if (this.targetedEntityID != null) {
            if (goalAction == null) {
                Logger.printError("Goal action is null, but targeted entity is not null!");
                targetedEntityID = null;
                return;
            }

            if (goalAction == GoalAction.ATTACK) {
                Entity target = this.world.getEntityByID(((Combatant) this).targetedEntityID);

                if (target.isDying == true) {
                    this.setTargetedEntityID(null);
                    this.setGoalAction(null);
                    this.stopAllMovement();
                    this.setTargetEntityLastPosition(null);
                    return;
                }
            }

            if (isOneStepAwayFromTarget()) {
                Entity entity = this.world.getEntityByID(this.targetedEntityID);
                if (entity != null) {
                    setTargetEntityLastPosition(null);
                    setFacingDirection(this.getDirectionTowardsTile(entity.worldX, entity.worldY));

                    switch (goalAction) {
                        case ATTACK -> {
                            if (entity instanceof Combatant combatant) {
                                ((Combatant) this).attackEntity(combatant);
                                stopAllMovement();
                            }
                        }
                        case TALK -> {
                            if (entity instanceof Npc npc) {
                                stopAllMovement();
                                this.setGoalAction(null);
                                this.setTargetedEntityID(null);

                                TalkEvent talkEvent = new TalkEvent(this.entityID, entity.entityID,
                                        npc.entityStaticData.entityIndex);

                                if (entity instanceof Combatant && ((Combatant) entity).targetedEntityID == null) {
                                    entity.setInteractionTargetID(this.entityID);
                                }

                                this.world.tickTalkEvents.add(talkEvent);
                            }
                        }
                        case TRADE -> {
                            if (entity instanceof Npc npc) {
                                this.stopAllMovement();
                                this.setGoalAction(null);
                                this.setTargetedEntityID(null);
                                TradeEvent tradeEvent = new TradeEvent(this.entityID, entity.entityID,
                                        npc.entityStaticData.entityIndex);
                                entity.setInteractionTargetID(this.entityID);
                                this.world.tickTradeEvents.add(tradeEvent);
                            }
                        }
                        default -> {
                        }
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

        return (Math.abs(this.worldX - target.worldX) == 1 && this.worldY == target.worldY)
                || (Math.abs(this.worldY - target.worldY) == 1 && this.worldX == target.worldX);
    }

    public void takeItem(String uniqueItemID) {
        Item item = this.world.itemsManager.getItemByUniqueItemID(uniqueItemID);
        if (item == null) {
            Logger.printError("Item not found");
            this.world.chatMessages
                    .add(new ChatMessage(this.username, "Too late, it's gone!", System.currentTimeMillis(), false));
            return;
        }
        boolean wasAddedToInventory = addItemToInventory(item.getItemID(), item.getAmount());
        if (wasAddedToInventory) {
            item.setIsDeleted(true);
            SoundEvent soundEvent = new SoundEvent("pick_up.ogg", true, false, this.entityID, false);
            this.world.tickSoundEvents.add(soundEvent);
        }

    }

    public void saveQuestProgress() {
        Gson gson = new Gson();
        String questProgressString = gson.toJson(this.questProgress);
        try {
            CommonQueries.savePlayerQuestProgressByAccountId(this.accountID, questProgressString);
        } catch (SQLException e) {
            Logger.printError("Failed to save quest progress");
        }
    }

    public void savePosition() {
        try {
            CommonQueries.savePlayerPositionByAccountId(this.accountID, this.worldX, this.worldY);
        } catch (SQLException e) {
            Logger.printError("Failed to save player position");
        }
    }

    public void saveInventory() {
        Gson gson = new Gson();
        String inventoryString = gson.toJson(this.inventory);
        String inventoryAmountsString = gson.toJson(this.inventoryAmounts);
        try {
            CommonQueries.savePlayerInventoryByAccountId(this.accountID, inventoryString, inventoryAmountsString);
        } catch (SQLException e) {
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
            ChatMessage chatMessageModel = new ChatMessage(this.username, levelUpMessage, timeSent, false);
            this.world.chatMessages.add(chatMessageModel);

            switch (skill) {
                case SkillUtils.ATTACK -> {
                    SoundEvent soundEvent = new SoundEvent("attack_level_up.ogg", true, true, this.entityID, false);
                    this.world.tickSoundEvents.add(soundEvent);
                }
                case SkillUtils.STRENGTH -> {
                    SoundEvent soundEvent = new SoundEvent("strength_level_up.ogg", true, true, this.entityID, false);
                    this.world.tickSoundEvents.add(soundEvent);
                }
                case SkillUtils.DEFENCE -> {
                    SoundEvent soundEvent = new SoundEvent("defence_level_up.ogg", true, true, this.entityID, false);
                    this.world.tickSoundEvents.add(soundEvent);
                }

                case SkillUtils.HITPOINTS -> {
                    SoundEvent soundEvent = new SoundEvent("hitpoints_level_up.ogg", true, true, this.entityID, false);
                    this.world.tickSoundEvents.add(soundEvent);
                }
                default -> {
                }
            }

        }
        this.skillsChanged = 1;
        saveSkillXp(skill);
    }

    public void saveSkillXp(int skill) {
        try {
            CommonQueries.savePlayerXpByAccountId(this.accountID, skill, this.skills[skill]);
        } catch (SQLException e) {
            Logger.printError("Failed to save skill xp");
        }
    }

    public void setTickActions(List<Action> actions) {
        for (Action action : actions) {

            if (action instanceof ChangeAppearanceAction changeAppearanceAction) {
                setSkinColor(changeAppearanceAction.getSkinColor());
                setHairColor(changeAppearanceAction.getHairColor());
                setShirtColor(changeAppearanceAction.getShirtColor());
                setPantsColor(changeAppearanceAction.getPantsColor());

                try {
                    CommonQueries.savePlayerAppearanceByAccountId(
                            this.accountID, this.skinColor, this.hairColor, this.shirtColor, this.pantsColor);
                } catch (SQLException e) {
                    Logger.printError(e.getMessage());
                }
            }

            if (action instanceof PlayerMove playerMove) {
                setNewTargetTile(new TilePosition(playerMove.getX(), playerMove.getY()));
                setTargetItemID(null);
                setTargetedEntityID(null);
                setGoalAction(null);
            }

            if (action instanceof PlayerAttackMove playerAttackMove) {
                Entity npc = this.world.getEntityByID(playerAttackMove.getEntityID());
                if (npc == null) {
                    Logger.printError("NPC not found");
                    return;
                }

                setNewTargetTile(new TilePosition(npc.worldX, npc.worldY));
                this.newTargetTile = new TilePosition(npc.worldX, npc.worldY);
                setTargetedEntityID(playerAttackMove.getEntityID());
                setGoalAction(GoalAction.ATTACK);
            }

            if (action instanceof DropItemAction dropItemAction) {
                this.dropItem(dropItemAction.getInventoryIndex());
            }

            if (action instanceof WieldItemAction wieldItemAction) {
                int itemID = this.inventory[wieldItemAction.getInventoryIndex()];
                Wieldable item = this.world.itemsManager.getWieldableInfoByItemID(itemID);

                if (item == null) {
                    Logger.printError("Item not found or not wieldable");
                    return;
                }

                switch (item.getType()) {
                    case "weapon" -> {
                        setWeapon(wieldItemAction.getInventoryIndex());
                        saveWieldables();
                    }
                    case "shield" -> {
                        setShield(wieldItemAction.getInventoryIndex());
                        saveWieldables();
                    }
                    default -> Logger.printError("Item is not wieldable");
                }

            }

            if (action instanceof PlayerTakeMoveAction playerTakeMoveAction) {
                this.handlePlayerTakeMove(playerTakeMoveAction.getUniqueItemID());
                setGoalAction(null);
            }

            if (action instanceof UseItemAction useItemAction) {
                this.useItem(useItemAction.getItemID(), useItemAction.getTargetID());
            }

            if (action instanceof UnwieldAction unwieldAction) {
                this.unwieldItem(unwieldAction.getInventoryIndex());
            }

            if (action instanceof EatItemAction eatItemAction) {
                this.eatItem(eatItemAction.getInventoryIndex());
            }

            if (action instanceof QuestProgressUpdateAction questProgressUpdateAction) {
                this.questProgressUpdate(questProgressUpdateAction.getQuestID(),
                        questProgressUpdateAction.getProgress());
                this.questProgressChanged = 1;
            }

            if (action instanceof PlayerTalkMoveAction playerTalkMoveAction) {
                Entity entity = this.world.getEntityByID(playerTalkMoveAction.getEntityID());
                if (entity != null) {
                    setTargetedEntityID(playerTalkMoveAction.getEntityID());
                    setGoalAction(GoalAction.TALK);
                    setTargetTile(new TilePosition(entity.worldX, entity.worldY));
                }
            }

            if (action instanceof ChangeAttackStyleAction changeAttackStyleAction) {
                setAttackStyle(changeAttackStyleAction.getAttackStyle());
            }

            if (action instanceof RemoveItemFromInventoryAction removeItemFromInventoryAction) {
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
                this.inventoryChanged = 1;
            }

            if (action instanceof AddItemToInventoryAction addItemToInventoryAction) {
                int itemID = addItemToInventoryAction.getItemID();
                int quantity = addItemToInventoryAction.getQuantity();
                this.addItemToInventory(itemID, quantity);
            }

            if (action instanceof ForceNpcAttackPlayerAction forceNpcAttackPlayerAction) {
                Entity entity = this.world.getEntityByID(forceNpcAttackPlayerAction.getNpcID());
                if (entity != null && entity instanceof Npc) {
                    ((Npc) entity).setTargetedEntityID(entityID);
                }
            }

            if (action instanceof BuyItemAction buyItemAction) {
                handleBuyItemAction(buyItemAction.getShopID(), buyItemAction.getItemID(),
                        buyItemAction.getAmount());
            }

            if (action instanceof SellItemAction sellItemAction) {
                handleSellItemAction(sellItemAction.getShopID(), sellItemAction.getInventoryIndex(),
                        sellItemAction.getAmount());
            }

            if (action instanceof TradeMoveAction tradeMoveAction) {
                Entity entity = this.world.getEntityByID(tradeMoveAction.getEntityID());
                if (entity != null) {
                    if (entity instanceof Player) {
                        // this.tradeWithPlayer((Player) entity); // TODO
                    } else {
                        setTargetedEntityID(tradeMoveAction.getEntityID());
                        setGoalAction(GoalAction.TRADE);
                        setNewTargetTile(new TilePosition(entity.worldX, entity.worldY));
                    }
                }
            }

            if (action instanceof SwapInventorySlots swapInventorySlotsAction) {
                handleSwapInventorySlotsAction(swapInventorySlotsAction.getFromSlot(),
                        swapInventorySlotsAction.getToSlot());
            }

        }
    }

    private void handleSwapInventorySlotsAction(int fromSlot, int toSlot) {

        // Bounds check
        if (fromSlot < 0 || toSlot < 0
                || fromSlot >= this.inventory.length
                || toSlot >= this.inventory.length) {
            Logger.printError("Invalid inventory slot swap: " + fromSlot + " -> " + toSlot);
            return;
        }

        // No-op
        if (fromSlot == toSlot) {
            return;
        }

        // Swap item IDs
        int tempItem = this.inventory[fromSlot];
        this.inventory[fromSlot] = this.inventory[toSlot];
        this.inventory[toSlot] = tempItem;

        // Swap stack amounts
        int tempAmount = this.inventoryAmounts[fromSlot];
        this.inventoryAmounts[fromSlot] = this.inventoryAmounts[toSlot];
        this.inventoryAmounts[toSlot] = tempAmount;

        // Fix weapon slot reference
        if (this.weapon == fromSlot) {
            this.weapon = toSlot;
            this.weaponChanged = 1;
        } else if (this.weapon == toSlot) {
            this.weapon = fromSlot;
            this.weaponChanged = 1;
        }

        // Fix shield slot reference
        if (this.shield == fromSlot) {
            this.shield = toSlot;
            this.shieldChanged = 1;
        } else if (this.shield == toSlot) {
            this.shield = fromSlot;
            this.shieldChanged = 1;
        }

        // Persist + sync
        saveInventory();
        this.inventoryChanged = 1;
        this.inventoryAmountsChanged = 1;
    }

    private void handleSellItemAction(String shopID, int inventoryIndex, int amount) {
        /*
         * // Validate input
         * if (amount <= 0) {
         * Logger.printError("Invalid sell quantity.");
         * return;
         * }
         * 
         * 
         * Shop shop = world.shopsManager.getShopByID(shopID);
         * if (shop == null) {
         * Logger.printError("Shop not found");
         * return;
         * }
         * 
         * int itemID = this.inventory[inventoryIndex];
         * Item item = world.itemsManager.getItemByID(itemID);
         * if (itemID == 0 || item == null) {
         * Logger.printError("Item not found in inventory");
         * return;
         * }
         * 
         * Stock stock = shop.getStock(itemID);
         * 
         * if (shop.getBuysAnything() == false) {
         * if (stock == null) {
         * world.chatMessages.add(new ChatMessage(this.username,
         * "The shop is not interested in that item.",
         * System.currentTimeMillis(), false));
         * return;
         * }
         * }
         * 
         * // Check how many items the player has
         * int playerItemQuantity = 0;
         * if (item.isStackable()) {
         * for (int i = 0; i < this.inventory.length; i++) {
         * if (this.inventory[i] == itemID) {
         * playerItemQuantity += this.inventoryAmounts[i];
         * }
         * }
         * } else {
         * for (int i = 0; i < this.inventory.length; i++) {
         * if (this.inventory[i] == itemID) {
         * playerItemQuantity++;
         * }
         * }
         * }
         * 
         * // If the player does not have enough items, sell all items they have
         * if (playerItemQuantity == 0) {
         * world.addChatMessage(new ChatMessage(this.username,
         * "You don't have any of that item to sell.",
         * System.currentTimeMillis(), false));
         * return;
         * }
         * 
         * // Set amount to sell to the available quantity, if amount is more than the
         * // player has
         * if (amount > playerItemQuantity) {
         * amount = playerItemQuantity;
         * }
         * 
         * // Calculate the total sell price
         * int sellPrice = (int) Math.floor(item.getValue() *
         * shop.getBuysAtPercentage());
         * long totalSellPrice = (long) sellPrice * amount;
         * if (totalSellPrice > Integer.MAX_VALUE) {
         * Logger.printError("Total price exceeds the maximum value.");
         * return;
         * }
         * 
         * // Deduct items from the player's inventory
         * int remainingAmount = amount;
         * if (item.isStackable()) {
         * for (int i = 0; i < this.inventory.length; i++) {
         * if (this.inventory[i] == itemID) {
         * if (this.inventoryAmounts[i] >= remainingAmount) {
         * this.inventoryAmounts[i] -= remainingAmount;
         * remainingAmount = 0;
         * if (this.inventoryAmounts[i] == 0) {
         * this.inventory[i] = 0;
         * }
         * break;
         * } else {
         * remainingAmount -= this.inventoryAmounts[i];
         * this.inventoryAmounts[i] = 0;
         * this.inventory[i] = 0;
         * }
         * }
         * }
         * } else {
         * for (int i = 0; i < this.inventory.length; i++) {
         * if (this.inventory[i] == itemID) {
         * this.inventory[i] = 0;
         * remainingAmount--;
         * if (remainingAmount == 0) {
         * break;
         * }
         * }
         * }
         * }
         * 
         * if (remainingAmount > 0) {
         * Logger.printError("Error while deducting items from inventory.");
         * return;
         * }
         * 
         * // Add coins to the player
         * addCoins((int) totalSellPrice);
         * 
         * // Update the shop's stock
         * // if shop already has item on stock-> add amount to stock
         * if (stock != null) {
         * if (stock.getQuantity() + amount > Integer.MAX_VALUE) {
         * world.addChatMessage(new ChatMessage(this.username,
         * "The shop cannot accept more of this item.", System.currentTimeMillis(),
         * false));
         * return;
         * }
         * 
         * stock.setQuantity(stock.getQuantity() + amount);
         * } else {
         * // if shop does not have item on stock -> create new stock
         * // -1 means no restocking
         * Stock newStock = new Stock(itemID, amount, -1);
         * newStock.setIsDefaultStock(false);
         * shop.addStock(newStock);
         * }
         * 
         * // Save changes to the inventory
         * saveInventory();
         */
    }

    private void addCoins(int totalSellPrice) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 102) {
                if ((long) this.inventoryAmounts[i] + totalSellPrice > Integer.MAX_VALUE) {
                    world.chatMessages.add(new ChatMessage(this.username,
                            "You already have a full stack of coins.",
                            System.currentTimeMillis(), false));
                    return;
                }
                this.inventoryAmounts[i] += totalSellPrice;
                saveInventory();
                this.inventoryChanged = 1;
                this.inventoryAmountsChanged = 1;

                return;
            }
        }

        int emptySlot = getEmptyInventorySlot();
        if (emptySlot == -1) {
            world.chatMessages.add(new ChatMessage(this.username,
                    "You don't have enough space in your inventory. The coins are dropped on the ground.",
                    System.currentTimeMillis(), false));
            world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, 102, 200, totalSellPrice);
            return;
        }

        this.inventory[emptySlot] = 102;
        this.inventoryAmounts[emptySlot] = totalSellPrice;
        saveInventory();
        this.inventoryChanged = 1;
        this.inventoryAmountsChanged = 1;
    }

    private void handleBuyItemAction(String shopID, int itemID, int amount) {
        /*
         * if (amount <= 0) {
         * Logger.printError("Invalid purchase quantity.");
         * return;
         * }
         * 
         * Shop shop = world.shopsManager.getShopByID(shopID);
         * 
         * if (shop == null) {
         * Logger.printError("Shop not found");
         * return;
         * }
         * 
         * Stock stock = shop.getStock(itemID);
         * if (stock == null) {
         * Logger.printError("Item not found in shop");
         * return;
         * }
         * 
         * int playerCoins = 0;
         * 
         * for (int i = 0; i < this.inventory.length; i++) {
         * if (this.inventory[i] == 102) {
         * playerCoins += this.inventoryAmounts[i];
         * }
         * }
         * 
         * Item item = world.itemsManager.getItemByID(itemID);
         * if (item == null) {
         * Logger.printError("Item not found in items manager, buy action failed.");
         * this.world.addChatMessage(
         * new ChatMessage(this.username, "Item not found", System.currentTimeMillis(),
         * false));
         * return;
         * }
         * 
         * int availableAmount = amount;
         * if (stock.getQuantity() < amount) {
         * if (stock.getQuantity() == 0) {
         * world.addChatMessage(
         * new ChatMessage(this.username, "The shop is out of stock.",
         * System.currentTimeMillis(),
         * false));
         * return;
         * }
         * availableAmount = stock.getQuantity();
         * }
         * 
         * int totalPrice = (int) Math.floor(item.getValue() *
         * shop.getSellsAtPercentage() * availableAmount);
         * 
         * if (totalPrice > Integer.MAX_VALUE) {
         * Logger.printError("Total price exceeds the maximum value.");
         * return;
         * }
         * 
         * if (playerCoins < totalPrice) {
         * world.addChatMessage(
         * new ChatMessage(this.username, "You don't have enough coins.",
         * System.currentTimeMillis(),
         * false));
         * return;
         * }
         * 
         * if (world.itemsManager.getItemByID(itemID).isStackable() == true) {
         * boolean isItemAlreadyInInventory = false;
         * for (int i = 0; i < this.inventory.length; i++) {
         * if (this.inventory[i] == itemID) {
         * if ((long) this.inventoryAmounts[i] + availableAmount > Integer.MAX_VALUE) {
         * Logger.printError("Quantity exceeds maximum limit for item stack.");
         * world.chatMessages.add(new ChatMessage(this.username,
         * "You already have a full stack of this item.",
         * System.currentTimeMillis(), false));
         * 
         * return;
         * }
         * this.inventoryAmounts[i] += availableAmount;
         * isItemAlreadyInInventory = true;
         * break;
         * }
         * }
         * 
         * if (isItemAlreadyInInventory == false) {
         * int emptySlot = getEmptyInventorySlot();
         * 
         * if (emptySlot == -1) {
         * world.chatMessages.add(new ChatMessage(this.username,
         * "You don't have enough space in your inventory.",
         * System.currentTimeMillis(), false));
         * return;
         * }
         * 
         * this.inventory[emptySlot] = itemID;
         * this.inventoryAmounts[emptySlot] = availableAmount;
         * }
         * 
         * } else {
         * for (int i = 0; i < availableAmount; i++) {
         * int emptySlot = getEmptyInventorySlot();
         * if (emptySlot == -1) {
         * world.addChatMessage(
         * new ChatMessage(this.username,
         * "You don't have enough space in your inventory.",
         * System.currentTimeMillis(), false));
         * return;
         * }
         * 
         * this.inventory[emptySlot] = itemID;
         * }
         * }
         * 
         * saveInventory();
         * stock.setQuantity(stock.getQuantity() - availableAmount);
         * if (stock.getQuantity() == 0 && stock.isDefaultStock() == false) {
         * shop.removeStock(itemID);
         * }
         * this.removeCoins(totalPrice);
         */
    }

    private void removeCoins(int amount) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 102) {
                if (this.inventoryAmounts[i] >= amount) {
                    this.inventoryAmounts[i] -= amount;
                    saveInventory();
                    this.inventoryChanged = 1;
                    this.inventoryAmountsChanged = 1;
                    return;
                } else {
                    world.addChatMessage(new ChatMessage(this.username, "You don't have enough coins.",
                            System.currentTimeMillis(), false));
                }
            }
        }
    }

    private boolean addItemToInventory(int itemID, int quantity) {
        Item item = this.world.itemsManager.getItemByID(itemID);
        if (item == null) {
            Logger.printError("Item not found");
            return false;
        }

        if (item.isStackable() == true) {
            boolean isItemAlreadyInInventory = false;
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    if ((long) this.inventoryAmounts[i] + quantity > Integer.MAX_VALUE) {
                        Logger.printError("Quantity exceeds maximum limit for item stack.");
                        world.chatMessages.add(new ChatMessage(this.username,
                                "You already have a full stack of this item.",
                                System.currentTimeMillis(), false));

                        return false;

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
                    world.chatMessages.add(new ChatMessage(this.username,
                            "You don't have enough space in your inventory.",
                            System.currentTimeMillis(), false));
                    return false;

                }

                this.inventory[emptySlot] = itemID;
                this.inventoryAmounts[emptySlot] = quantity;
            }
        } else {
            if (!item.isStackable() && quantity > 1) {
                Logger.printError("Cannot add multiple non-stackable items.");
                return false;

            }
            int emptySlot = getEmptyInventorySlot();
            Logger.printError(Integer.toString(emptySlot));
            if (emptySlot == -1) {
                world.chatMessages.add(new ChatMessage(this.username,
                        "You don't have enough space in your inventory.",
                        System.currentTimeMillis(), false));
                return false;

            }

            this.inventory[emptySlot] = itemID;
        }

        saveInventory();
        this.inventoryChanged = 1;
        this.inventoryAmountsChanged = 1;
        return true;
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
        setCurrentHitpoints(currentHitpoints += edible.getHealAmount());
        this.world.chatMessages
                .add(new ChatMessage(this.username, "You eat the " + item.getName() + ". " + "It heals some health.",
                        System.currentTimeMillis(),
                        false));

        if (this.currentHitpoints > ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS])) {
            this.currentHitpoints = ExperienceUtils.getLevelByExp(this.skills[SkillUtils.HITPOINTS]);
        }

        SoundEvent soundEvent = new SoundEvent("eat.ogg", true, false, this.entityID, false);
        this.world.tickSoundEvents.add(soundEvent);
        setAttackTickCounter(4);

    }

    // TODO add item use functionality
    private void useItem(int itemID, int targetID) {
        this.world.chatMessages
                .add(new ChatMessage(this.username, "Nothing interesting happens.", System.currentTimeMillis(), false));

    }

    private void handlePlayerTakeMove(String uniqueItemID) {
        Item item = this.world.itemsManager.getItemByUniqueItemID(uniqueItemID);
        if (item == null) {
            this.world.chatMessages
                    .add(new ChatMessage(this.username, "Too late, it's gone!", System.currentTimeMillis(), false));
            return;
        }

        if (item.getWorldX() == this.worldX && item.getWorldY() == this.worldY) {
            this.takeItem(uniqueItemID);
            return;
        }

        setTargetItemID(uniqueItemID);
        setNewTargetTile(new TilePosition(item.getWorldX(), item.getWorldY()));
    }

    private void questProgressUpdate(int questID, int progress) {
        if (questID < 0 || questID >= this.questProgress.length) {
            Logger.printError("Invalid quest ID");
            return;
        }

        this.questProgress[questID] = progress;
        saveQuestProgress();

        if (progress == 100) { // 100 is the completion value
            ChatMessage chatMessage = new ChatMessage(this.username, "Congratulations, you've completed a quest!",
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
                this.inventoryChanged = 1;
                this.inventoryAmountsChanged = 1;
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
            CommonQueries.savePlayerWieldablesByAccountId(this.accountID, this.weapon, this.shield);
        } catch (SQLException e) {
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

        if (this.weapon != -1 && this.weapon == inventoryIndex) {
            setWeapon(-1);
            saveWieldables();
        }

        if (this.shield != -1 && this.shield == inventoryIndex) {
            setShield(-1);
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

        SoundEvent soundEvent = new SoundEvent("drop.ogg", true, false, this.entityID, false);
        this.world.tickSoundEvents.add(soundEvent);
        saveInventory();
        this.inventoryChanged = 1;
        this.inventoryAmountsChanged = 1;
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

        if (this.weapon != -1 && this.weapon == inventoryIndex) {
            setWeapon(-1);
        } else if (this.shield != -1 && this.shield == inventoryIndex) {
            setShield(-1);
        } else {
            Logger.printError("Item not wielded");
            return;
        }

        saveWieldables();

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
            setDyingCounter(++dyingCounter);
            if (dyingCounter > 5) {
                resetPlayer();
                setIsDying(false);
                setDyingCounter(0);
            }
        }

    }

    private void loadPlayerSkills(DBPlayer player) {
        this.skills[SkillUtils.ATTACK] = player.getAttackExperience();
        this.skills[SkillUtils.STRENGTH] = player.getStrengthExperience();
        this.skills[SkillUtils.DEFENCE] = player.getDefenceExperience();
        this.skills[SkillUtils.HITPOINTS] = player.getHitpointsExperience();
    }

    private void loadPlayerInventory(DBPlayer player) {
        this.inventory = new int[12];
        this.inventoryAmounts = new int[12];
        System.arraycopy(player.getInventory(), 0, this.inventory, 0, player.getInventory().length);
        System.arraycopy(player.getInventoryAmounts(), 0, this.inventoryAmounts, 0,
                player.getInventoryAmounts().length);
    }

    public void killPlayer() {
        setIsDying(true);
        stopAllMovement();
        this.world.chatMessages
                .add(new ChatMessage(this.username, "Oh dear, you are dead!", System.currentTimeMillis(), false));
    }

    public void resetPlayer() {
        setWeapon(-1);
        setShield(-1);

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

        this.saveInventory();
        this.inventoryChanged = 1;
        this.inventoryAmountsChanged = 1;
        this.saveWieldables();
    }

    public void setInfluence(int influence) {
        this.influence = influence;
        this.influenceChanged = 1;
    }

    public void setSkinColor(int skinColor) {
        this.skinColor = skinColor;
        this.skinColorChanged = 1;
    }

    public void setHairColor(int hairColor) {
        this.hairColor = hairColor;
        this.hairColorChanged = 1;
    }

    public void setShirtColor(int shirtColor) {
        this.shirtColor = shirtColor;
        this.shirtColorChanged = 1;
    }

    public void setPantsColor(int pantsColor) {
        this.pantsColor = pantsColor;
        this.pantsColorChanged = 1;
    }

}
