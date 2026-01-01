package com.g8e.gameserver.models;

public class AttackStyle {
    private String name;
    private int value;

    public AttackStyle(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

}
