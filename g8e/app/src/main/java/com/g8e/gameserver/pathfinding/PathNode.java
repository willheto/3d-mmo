package com.g8e.gameserver.pathfinding;

public class PathNode {
    public int x;
    public int y;
    int g;
    int h;
    int f;
    PathNode parent;

    public PathNode(int x, int y, PathNode parent) {
        this.x = x;
        this.y = y;
        this.g = 0;
        this.h = 0;
        this.f = 0;
        this.parent = parent;
    }

    public void calculateCosts(PathNode target) {
        this.h = Math.abs(this.x - target.x) + Math.abs(this.y - target.y);
        this.f = this.g + this.h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PathNode))
            return false;
        PathNode n = (PathNode) o;
        return x == n.x && y == n.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

}