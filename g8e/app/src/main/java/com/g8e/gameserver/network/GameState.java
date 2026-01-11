package com.g8e.gameserver.network;

import java.util.List;

import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.events.AttackEvent;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.events.TradeEvent;
import com.g8e.gameserver.network.dataTransferModels.DTOItem;
import com.g8e.gameserver.network.dataTransferModels.DTONpc;
import com.g8e.gameserver.network.dataTransferModels.DTOPlayer;

public class GameState {
    private List<AttackEvent> tickAttackEvents;
    private List<TalkEvent> tickTalkEvents;
    public List<TradeEvent> tickTradeEvents;
    public List<SoundEvent> tickSoundEvents;
    private List<DTOPlayer> players;
    private List<DTONpc> npcs;
    private List<ChatMessage> chatMessages;
    private String playerID;
    private List<DTOItem> items;
    private final List<String> onlinePlayers;

    public GameState(List<AttackEvent> tickAttackEvents, List<TalkEvent> tickTalkEvents,
            List<TradeEvent> tickTradeEvents,
            List<SoundEvent> tickSoundEvents,
            List<DTOPlayer> players,
            List<DTONpc> npcs,
            List<ChatMessage> chatMessages, List<DTOItem> items, String playerID, List<String> onlinePlayers) {
        this.tickAttackEvents = tickAttackEvents;
        this.tickTalkEvents = tickTalkEvents;
        this.tickTradeEvents = tickTradeEvents;
        this.tickSoundEvents = tickSoundEvents;
        this.players = players;
        this.npcs = npcs;
        this.playerID = playerID;
        this.chatMessages = chatMessages;
        this.items = items;
        this.onlinePlayers = onlinePlayers;
    }

    public List<TradeEvent> getTickTradeEvents() {
        return tickTradeEvents;
    }

    public List<String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public List<DTOPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<DTOPlayer> players) {
        this.players = players;
    }

    public List<DTONpc> getNpcs() {
        return npcs;
    }

    public void setNpcs(List<DTONpc> npcs) {
        this.npcs = npcs;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    public List<AttackEvent> getTickAttackEvents() {
        return tickAttackEvents;
    }

    public void setTickAttackEvents(List<AttackEvent> tickAttackEvents) {
        this.tickAttackEvents = tickAttackEvents;
    }

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    public List<DTOItem> getItems() {
        return items;
    }

    public List<TalkEvent> getTickTalkEvents() {
        return tickTalkEvents;
    }

    public void setTickTalkEvents(List<TalkEvent> tickTalkEvents) {
        this.tickTalkEvents = tickTalkEvents;
    }

    public List<SoundEvent> getTickSoundEvents() {
        return tickSoundEvents;
    }

    public void setTickTradeEvents(List<TradeEvent> tickTradeEvents) {
        this.tickTradeEvents = tickTradeEvents;
    }

    public void setTickSoundEvents(List<SoundEvent> tickSoundEvents) {
        this.tickSoundEvents = tickSoundEvents;
    }

    public void setItems(List<DTOItem> items) {
        this.items = items;
    }
}