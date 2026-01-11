package com.g8e.gameserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import com.g8e.gameserver.constants.NpcConstants;
import com.g8e.gameserver.managers.EntitiesManager;
import com.g8e.gameserver.managers.ItemsManager;
import com.g8e.gameserver.managers.QuestsManager;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.entities.Entity;
import com.g8e.gameserver.models.entities.Npc;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.events.AttackEvent;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.events.TradeEvent;
import com.g8e.gameserver.models.objects.Item;
import com.g8e.gameserver.network.GameState;
import com.g8e.gameserver.network.WebSocketEventsHandler;
import com.g8e.gameserver.network.actions.Action;
import com.g8e.gameserver.network.compressing.Compress;
import com.g8e.gameserver.network.dataTransferModels.DTOItem;
import com.g8e.gameserver.network.dataTransferModels.DTONpc;
import com.g8e.gameserver.network.dataTransferModels.DTOPlayer;
import com.g8e.gameserver.tile.TileManager;
import com.g8e.util.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class World {
    private static final int TICK_RATE = 600;
    public final int maxWorldCol = 20;
    public final int maxWorldRow = 20;
    public final int maxPlayers = 1000;

    public WebSocketEventsHandler webSocketEventsHandler;
    public TileManager tileManager = new TileManager(this);
    public ItemsManager itemsManager = new ItemsManager(this);
    public EntitiesManager entitiesManager = new EntitiesManager();
    public QuestsManager questsManager = new QuestsManager();
    public List<Player> players = new ArrayList<>();
    public List<Npc> npcs = new ArrayList<>();
    public List<Item> items = new ArrayList<>();
    public List<ChatMessage> chatMessages = new ArrayList<>();
    public List<Action> actionQueue = new ArrayList<>();
    public List<AttackEvent> tickAttackEvents = new ArrayList<>();
    public List<TalkEvent> tickTalkEvents = new ArrayList<>();
    public List<TradeEvent> tickTradeEvents = new ArrayList<>();
    public List<SoundEvent> tickSoundEvents = new ArrayList<>();

    public WebSocket[] connections = new WebSocket[maxPlayers];
    public List<String> onlinePlayers = new ArrayList<>();

    public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> combatChecks = new ConcurrentHashMap<>();

    public World() {
        this.setInitialNpcs();
        this.setInitialItems();
    }

    public WebSocket[] getConnections() {
        return connections;
    }

    public List<String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public void addConnection(WebSocket conn) {
        for (int i = 0; i < maxPlayers; i++) {
            if (connections[i] == null) {
                connections[i] = conn;
                onlinePlayers.add(conn.toString());
                break;
            }
        }
    }

    public void removeConnection(WebSocket conn) {
        for (int i = 0; i < maxPlayers; i++) {
            if (connections[i] == conn) {
                connections[i] = null;
                onlinePlayers.remove(conn.toString());
                break;
            }
        }
    }

    public void start() {
        while (true) {
            try {
                Thread.sleep(TICK_RATE);
                gameTick();
            } catch (InterruptedException e) {
                Logger.printError(e.getMessage());
            }
        }
    }

    public void addChatMessage(ChatMessage chatMessage) {
        this.chatMessages.add(chatMessage);
        Logger.printDebug(chatMessage.getMessage());
    }

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    private void gameTick() {
        try {
            this.players.forEach(player -> {
                List<Action> playerActions = this.actionQueue.stream()
                        .filter(action -> action.getPlayerID().equals(player.entityID))
                        .toList();

                player.setTickActions(playerActions);
                player.update();
            });

            // Remove actions after processing all players to avoid concurrent modification
            // exception
            List<Action> actionsToRemove = this.players.stream()
                    .flatMap(player -> this.actionQueue.stream()
                            .filter(action -> action.getPlayerID().equals(player.entityID)))
                    .toList();
            this.actionQueue.removeAll(actionsToRemove);

            this.npcs.forEach(npc -> {
                npc.update();
            });
            itemsManager.updateDespawnTimers();
            sentGameStateToConnections();
            cleanUpData();
        } catch (Exception e) {
            Logger.printError(e.getMessage());
        }
    }

    private void cleanUpData() {
        this.chatMessages.clear();

        this.players.forEach(player -> {
            player.clearChangedFlags();
        });

        this.npcs.forEach(npc -> {
            npc.clearChangedFlags();
        });

        this.items.forEach(npc -> {
            npc.clearChangedFlags();
        });
    }

    private void sentGameStateToConnections() {
        List<DTOPlayer> dtoPlayers = this.players.stream()
                .map(DTOPlayer::new)
                .filter(dto -> !dto.hasOnlyEntityId())
                .toList();
        List<DTONpc> dtoNpcs = this.npcs.stream()
                .map(DTONpc::new)
                .filter(dto -> !dto.hasOnlyEntityId())
                .toList();

        List<DTOItem> dtoItems = this.items.stream()
                .map(DTOItem::new)
                .filter(dto -> !dto.hasOnlyUniqueId())
                .toList();

        GameState newGameState = new GameState(this.tickAttackEvents, this.tickTalkEvents, this.tickTradeEvents,
                this.tickSoundEvents,
                dtoPlayers, dtoNpcs,
                this.chatMessages,
                dtoItems, null, this.onlinePlayers);

        for (WebSocket conn : connections) {
            if (conn != null) {
                Player player = this.players.stream().filter(p -> p.entityID.equals(conn.toString())).findFirst()
                        .orElse(null);

                if (player != null) {
                    removeEmptyCollections(newGameState);
                    Gson gson = new GsonBuilder().create();
                    String gameStateJson = gson.toJson(newGameState);
                    byte[] compressedData = Compress.compress(gameStateJson);

                    try {
                        conn.send(compressedData);
                    } catch (WebsocketNotConnectedException e) {
                        Logger.printInfo("Connection " + conn
                                + " is not connected, probably in combat and waiting to be logged out");
                    }

                }
            }
        }

        this.tickAttackEvents.clear();
        this.tickTalkEvents.clear();
        this.tickTradeEvents.clear();
        this.tickSoundEvents.clear();
    }

    private void removeEmptyCollections(GameState state) {
        if (state.getTickAttackEvents().isEmpty())
            state.setTickAttackEvents(null);
        if (state.getTickTalkEvents().isEmpty())
            state.setTickTalkEvents(null);
        if (state.getTickTradeEvents().isEmpty())
            state.setTickTradeEvents(null);
        if (state.getTickSoundEvents().isEmpty())
            state.setTickSoundEvents(null);

        if (state.getPlayers().isEmpty())
            state.setPlayers(null);
        if (state.getNpcs().isEmpty())
            state.setNpcs(null);
        if (state.getItems().isEmpty())
            state.setItems(null);
        if (state.getChatMessages().isEmpty())
            state.setChatMessages(null);

    }

    public List<Item> getItems() {
        return items;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Npc> getNpcs() {
        return npcs;
    }

    public void enqueueAction(Action action) {
        this.actionQueue.add(action);
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public Entity getEntityByID(String entityID) {
        for (Player player : players) {
            if (entityID.equals(player.entityID)) {
                return player;
            }
        }

        for (Npc npc : npcs) {
            if (entityID.equals(npc.entityID)) {
                return npc;
            }
        }

        return null;
    }

    public Item getItemByID(String itemUniqueID) {
        Item item = null;
        for (Item i : items) {
            if (i != null && i.getUniqueID().equals(itemUniqueID)) {
                item = i;
                break;
            }
        }
        return item;
    }

    public void removePlayer(WebSocket conn) {
        String playerID = conn.toString();
        Player player = this.players.stream().filter(p -> p.entityID.equals(playerID)).findFirst().orElse(null);
        if (player != null) {
            if (player.isInCombat == false) {
                this.players.remove(player);
                removeConnection(conn);
            } else {
                // Schedule a task to check every 600 milliseconds if the player is still in
                // combat
                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                    if (!player.isInCombat) {
                        players.remove(player);
                        removeConnection(conn);

                        System.out.println("Player removed from game after combat ended");

                        // Cancel this specific task
                        combatChecks.get(playerID).cancel(true);
                        combatChecks.remove(playerID);
                    }
                }, 0, 600, TimeUnit.MILLISECONDS);

                // Store the ScheduledFuture in the map
                combatChecks.put(playerID, future);
            }

            // set interval to check if player is still in combat

        }
    }

    private void setInitialNpcs() {
        for (int i = 0; i < 3; i++) {
            addNpc(NpcConstants.MAN, 5, 17, 7);
        }
    }

    private void addNpc(int index, int x, int y, int wanderRange) {
        Npc npc = new Npc(this, index, x, y);
        this.npcs.add(npc);
        npc.setWanderRange(wanderRange);
    }

    private void setInitialItems() {
        this.itemsManager.spawnItem(0, 0, 100);
        this.itemsManager.spawnItem(1, 1, 101);
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Action> getActionQueue() {
        return actionQueue;
    }

    public List<TalkEvent> getTickTalkEvents() {
        return tickTalkEvents;
    }

    public List<TradeEvent> getTickTradeEvents() {
        return tickTradeEvents;
    }

    public List<SoundEvent> getTickSoundEvents() {
        return tickSoundEvents;
    }

    public void setTickSoundEvents(List<SoundEvent> tickSoundEvents) {
        this.tickSoundEvents = tickSoundEvents;
    }

}
