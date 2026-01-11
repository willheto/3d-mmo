package com.g8e.gameserver.network;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;

import com.g8e.db.CommonQueries;
import com.g8e.db.models.DBAccount;
import com.g8e.db.models.DBPlayer;
import com.g8e.gameserver.World;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.events.AttackEvent;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.events.TradeEvent;
import com.g8e.gameserver.network.actions.Action;
import com.g8e.gameserver.network.actions.ChangeAppearanceAction;
import com.g8e.gameserver.network.actions.attackStyle.ChangeAttackStyleAction;
import com.g8e.gameserver.network.actions.chat.ChatMessageAction;
import com.g8e.gameserver.network.actions.drop.DropItemAction;
import com.g8e.gameserver.network.actions.edibles.EatItemAction;
import com.g8e.gameserver.network.actions.inventory.AddItemToInventoryAction;
import com.g8e.gameserver.network.actions.inventory.RemoveItemFromInventoryAction;
import com.g8e.gameserver.network.actions.move.ForceNpcAttackPlayerAction;
import com.g8e.gameserver.network.actions.move.PlayerAttackMove;
import com.g8e.gameserver.network.actions.move.PlayerAttackMoveData;
import com.g8e.gameserver.network.actions.move.PlayerMove;
import com.g8e.gameserver.network.actions.move.PlayerMoveData;
import com.g8e.gameserver.network.actions.move.PlayerTakeMoveAction;
import com.g8e.gameserver.network.actions.move.PlayerTalkMoveAction;
import com.g8e.gameserver.network.actions.quest.QuestProgressUpdateAction;
import com.g8e.gameserver.network.actions.shop.BuyItemAction;
import com.g8e.gameserver.network.actions.shop.SellItemAction;
import com.g8e.gameserver.network.actions.shop.TradeMoveAction;
import com.g8e.gameserver.network.actions.use.UseItemAction;
import com.g8e.gameserver.network.actions.wield.UnwieldAction;
import com.g8e.gameserver.network.actions.wield.WieldItemAction;
import com.g8e.gameserver.network.compressing.Compress;
import com.g8e.gameserver.network.dataTransferModels.DTOItem;
import com.g8e.gameserver.network.dataTransferModels.DTONpc;
import com.g8e.gameserver.network.dataTransferModels.DTOPlayer;
import com.g8e.util.Logger;
import com.google.gson.Gson;

public class WebSocketEventsHandler {
    private final World world;

    public WebSocketEventsHandler(World world) {
        this.world = world;

    }

    public void handleConnection(WebSocket conn, Map<String, String> queryParams) {
        String loginToken = queryParams.get("loginToken");
        if (loginToken == null) {
            Logger.printError("Player connected without login token");
            conn.close();
            return;
        }

        DBAccount account;
        try {
            account = CommonQueries.getAccountByLoginToken(loginToken);
            if (account == null) {
                Logger.printError("Player connected with invalid login token");
                conn.close();
                return;
            }

            DBPlayer player;

            player = CommonQueries.getPlayerByAccountId(account.getAccountId());

            if (player == null) {
                Logger.printError("Player not found");
                conn.close();
                return;
            }

            world.addConnection(conn);

            String uniquePlayerID = conn.toString();

            Player playerToBeAdded = new Player(this.world, player, uniquePlayerID, account.getUsername(),
                    account.getAccountId());

            world.addPlayer(playerToBeAdded);

            List<DTONpc> npcs = this.world.npcs.stream().map(p -> new DTONpc(p, true))
                    .toList();
            List<DTOPlayer> dtoPlayers = this.world.players.stream().map(p -> new DTOPlayer(p, true))
                    .toList();
            List<DTOItem> dtoItems = this.world.items.stream().map(p -> new DTOItem(p, true))
                    .toList();
            List<AttackEvent> attackEvents = new ArrayList<>();
            List<TalkEvent> talkEvents = new ArrayList<>();
            List<TradeEvent> tradeEvents = new ArrayList<>();
            List<SoundEvent> soundEvents = new ArrayList<>();

            GameState gameState = new GameState(attackEvents, talkEvents, tradeEvents,
                    soundEvents,
                    dtoPlayers,
                    npcs,
                    world.getChatMessages(),
                    dtoItems,
                    conn.toString(),
                    world.getOnlinePlayers());

            String gameStateJson = new Gson().toJson(gameState);
            byte[] compressedData = Compress.compress(gameStateJson);

            conn.send(compressedData);
            addDefaultChatMessages(playerToBeAdded.username);

        } catch (SQLException e) {
            Logger.printError(loginToken + " failed to connect to the game server");
            Logger.printError(e.getMessage());
        }
    }

    private void addDefaultChatMessages(String name) {
        ChatMessage welcomeMessage = new ChatMessage(name, "Welcome to the game!",
                System.currentTimeMillis(),
                false);

        ChatMessage tutorialMessage = new ChatMessage(name,
                "You can interact with the world using your mouse.", System.currentTimeMillis(), false);

        world.addChatMessage(welcomeMessage);
        world.addChatMessage(tutorialMessage);
    }

    public void handleMessage(WebSocket conn, String message) {
        Gson gson = new Gson();
        Action parsedMessage = gson.fromJson(message, Action.class);
        String action = parsedMessage.getAction();
        String playerID = parsedMessage.getPlayerID();

        switch (action) {
            case "logOut" -> {
                this.world.removePlayer(conn);
                conn.close();
            }
            case "ping" -> conn.send("pong");
            case "changeAppearance" -> {
                ChangeAppearanceAction changeAppearanceAction = gson.fromJson(message, ChangeAppearanceAction.class);
                this.world.enqueueAction(changeAppearanceAction);
            }
            case "playerMove" -> {
                PlayerMove playerMoveAction = gson.fromJson(message, PlayerMove.class);
                int x = playerMoveAction.getX();
                int y = playerMoveAction.getY();

                this.world.enqueueAction(new PlayerMove(playerID, new PlayerMoveData(x, y)));
            }

            case "playerAttackMove" -> {
                PlayerAttackMove playerAttackMoveAction = gson.fromJson(message, PlayerAttackMove.class);

                String entityID = playerAttackMoveAction.getEntityID();
                this.world.enqueueAction(
                        new PlayerAttackMove(playerID, new PlayerAttackMoveData(entityID)));
            }

            case "chatMessage" -> {
                ChatMessageAction chatMessage = gson.fromJson(message, ChatMessageAction.class);
                Player player = this.world.players.stream().filter(p -> p.entityID.equals(playerID)).findFirst().get();
                String senderName = player != null ? player.username : "";
                ChatMessage chatMessageModel = new ChatMessage(senderName, chatMessage.getMessage(),
                        chatMessage.getTimeSent(), chatMessage.isGlobal());
                this.world.addChatMessage(chatMessageModel);
            }

            case "dropItem" -> {
                DropItemAction dropItemAction = gson.fromJson(message, DropItemAction.class);
                this.world.enqueueAction(dropItemAction);
            }

            case "wieldItem" -> {
                WieldItemAction wieldItemAction = gson.fromJson(message, WieldItemAction.class);
                this.world.enqueueAction(wieldItemAction);
            }

            case "unwieldItem" -> {
                UnwieldAction unwieldItemAction = gson.fromJson(message, UnwieldAction.class);
                this.world.enqueueAction(unwieldItemAction);
            }
            case "playerTakeMove" -> {
                PlayerTakeMoveAction playerTakeMoveAction = gson.fromJson(message, PlayerTakeMoveAction.class);
                this.world.enqueueAction(playerTakeMoveAction);
            }

            case "useItem" -> {
                UseItemAction useItemAction = gson.fromJson(message, UseItemAction.class);
                this.world.enqueueAction(useItemAction);
            }

            case "eatItem" -> {
                EatItemAction eatItemAction = gson.fromJson(message, EatItemAction.class);
                this.world.enqueueAction(eatItemAction);
            }
            case "questProgressUpdate" -> {
                QuestProgressUpdateAction questProgressUpdateAction = gson.fromJson(message,
                        QuestProgressUpdateAction.class);
                this.world.enqueueAction(questProgressUpdateAction);
            }
            case "playerTalkMove" -> {
                PlayerTalkMoveAction playerTalkMoveAction = gson.fromJson(message, PlayerTalkMoveAction.class);
                this.world.enqueueAction(playerTalkMoveAction);
            }
            case "changeAttackStyle" -> {
                ChangeAttackStyleAction changeAttackStyleAction = gson.fromJson(message, ChangeAttackStyleAction.class);
                this.world.enqueueAction(changeAttackStyleAction);
            }
            case "removeItemFromInventory" -> {
                RemoveItemFromInventoryAction removeItemFromInventoryAction = gson.fromJson(message,
                        RemoveItemFromInventoryAction.class);
                this.world.enqueueAction(removeItemFromInventoryAction);
            }
            case "addItemToInventory" -> {
                AddItemToInventoryAction addItemToInventoryAction = gson.fromJson(message,
                        AddItemToInventoryAction.class);
                this.world.enqueueAction(addItemToInventoryAction);
            }
            case "forceNpcAttackPlayer" -> {
                ForceNpcAttackPlayerAction forceNpcAttackPlayer = gson.fromJson(message,
                        ForceNpcAttackPlayerAction.class);
                this.world.enqueueAction(forceNpcAttackPlayer);
            }
            case "buyItem" -> {
                BuyItemAction buyItemAction = gson.fromJson(message, BuyItemAction.class);
                this.world.enqueueAction(buyItemAction);
            }

            case "sellItem" -> {
                SellItemAction sellItemAction = gson.fromJson(message, SellItemAction.class);
                this.world.enqueueAction(sellItemAction);
            }
            case "tradeMove" -> {
                TradeMoveAction tradeMoveAction = gson.fromJson(message, TradeMoveAction.class);
                this.world.enqueueAction(tradeMoveAction);
            }

            default -> {
            }
        }

    }

}
