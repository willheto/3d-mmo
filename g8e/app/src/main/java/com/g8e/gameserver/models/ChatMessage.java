package com.g8e.gameserver.models;

public class ChatMessage {
    private final String senderName;
    private final String message;
    private final long timeSent;
    private final boolean isGlobal;

    public ChatMessage(String senderName, String message, long timeSent, boolean isGlobal) {
        this.senderName = senderName;
        this.message = message;
        this.timeSent = timeSent;
        this.isGlobal = isGlobal;
    }

    public String getMessage() {
        return message;
    }

    public long getTimeSent() {
        return timeSent;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public String getSenderName() {
        return senderName;
    }
}
