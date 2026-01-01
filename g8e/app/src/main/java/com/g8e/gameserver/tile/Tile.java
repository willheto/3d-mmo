package com.g8e.gameserver.tile;

public class Tile {

    public boolean collision = false;
    public int numberRepresentation;

    public Tile(boolean collision, int numberRepresentation) {
        this.collision = collision;
        this.numberRepresentation = numberRepresentation;
    }

}
