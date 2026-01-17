package com.g8e.gameserver.models.entities;

import java.util.List;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.enums.GoalAction;
import com.g8e.gameserver.pathfinding.AStar;
import com.g8e.gameserver.pathfinding.PathNode;
import com.g8e.gameserver.tile.TilePosition;

public abstract class Entity {
    public String entityID;
    public int worldX;
    public int worldY;
    public Direction facingDirection = Direction.DOWN;

    public transient World world;
    public transient AStar pathFinder;

    public transient int originalWorldX; // Where entity respawns
    public transient int originalWorldY; // Where entity respawns

    protected transient TilePosition targetEntityLastPosition;

    public transient int followCounter = 0;
    public transient int shouldFollow = 0;
    public transient int dyingCounter = 0;

    public transient String targetItemID = null;
    public transient String interactionTargetID = null;

    protected transient GoalAction goalAction;

    public transient int wanderRange = 5;
    public transient int interactionRange = 1;

    public boolean isDying = false;

    protected transient TilePosition[] waypoints = new TilePosition[25];
    protected transient int waypointIndex = -1;

    protected transient Direction walkDirection = Direction.NONE;

    public int lastTickX;
    public int lastTickY;

    public int entityIDChanged = 1;

    public int worldXChanged = 1;
    public int worldYChanged = 1;
    public int facingDirectionChanged = 1;

    public int targetEntityLastPositionChanged = 1;

    public int followCounterChanged = 1;
    public int shouldFollowChanged = 1;
    public int dyingCounterChanged = 1;

    public int targetItemIDChanged = 1;
    public int interactionTargetIDChanged = 1;

    public int goalActionChanged = 1;

    public int wanderRangeChanged = 1;
    public int interactionRangeChanged = 1;

    public int isDyingChanged = 1;

    public Entity(String entityID, World world, int worldX, int worldY) {
        this.goalAction = null;
        this.entityID = entityID;
        this.world = world;
        this.originalWorldX = worldX;
        this.originalWorldY = worldY;
        this.worldX = worldX;
        this.worldY = worldY;
        this.pathFinder = new AStar(world);
    }

    public abstract void update();

    protected void queuePath(List<PathNode> path) {
        waypointIndex = -1;

        // push reversed so we pop from the end
        for (int i = path.size() - 1; i >= 0 && waypointIndex + 1 < waypoints.length; i--) {
            PathNode n = path.get(i);
            waypoints[++waypointIndex] = new TilePosition(n.x, n.y);
        }
    }

    protected boolean hasWaypoints() {
        return waypointIndex >= 0;
    }

    protected void clearWaypoints() {
        waypointIndex = -1;
        walkDirection = Direction.NONE;
    }

    protected boolean processMovement() {
        if (!hasWaypoints()) {
            return false;
        }

        Direction dir = takeStep();
        if (dir == Direction.NONE) {
            clearWaypoints();
            return false;
        }

        moveOneTile(dir);
        return true;
    }

    protected Direction takeStep() {
        if (waypointIndex < 0) {
            return Direction.NONE;
        }

        TilePosition target = waypoints[waypointIndex];

        int dx = Integer.compare(target.x, worldX);
        int dy = Integer.compare(target.y, worldY);

        // reached this waypoint
        if (dx == 0 && dy == 0) {
            waypointIndex--;
            return takeStep();
        }

        Direction dir = getDirection(dx, dy);

        if (!canMove(dir)) {
            return Direction.NONE;
        }

        return dir;
    }

    protected boolean canMove(Direction dir) {
        int nx = worldX;
        int ny = worldY;

        Entity occupying = world.getEntityAt(nx, ny);
        if (occupying != null && occupying != this) {
            return false;
        }

        switch (dir) {
            case UP -> ny--;
            case DOWN -> ny++;
            case LEFT -> nx--;
            case RIGHT -> nx++;
            default -> {
                return false;
            }
        }

        return !world.tileManager.getCollisionByXandY(nx, ny);
    }

    protected void moveOneTile(Direction dir) {
        setFacingDirection(dir);

        lastTickX = worldX;
        lastTickY = worldY;

        switch (dir) {
            case UP -> worldY--;
            case DOWN -> worldY++;
            case LEFT -> worldX--;
            case RIGHT -> worldX++;
        }

        setWorldX(worldX);
        setWorldY(worldY);
    }

    protected void moveTo(TilePosition target) {
        List<PathNode> path = pathFinder.findPath(worldX, worldY, target.x, target.y);

        if (path == null || path.size() < 2) {
            return;
        }

        queuePath(path);
    }

    protected Direction getDirectionTowardsTile(int entityX, int entityY) {
        if (entityX < this.worldX) {
            return Direction.LEFT;
        } else if (entityX > this.worldX) {
            return Direction.RIGHT;
        } else if (entityY < this.worldY) {
            return Direction.UP;
        } else {
            return Direction.DOWN;
        }

    }

    // Always use move instead of explicitly setting worldX and worldY
    // This will ensure that the chunk is updated correctly
    protected void move(int worldX, int worldY) {
        setWorldX(worldX);
        setWorldY(worldY);
        if (this instanceof Player player) {
            player.savePosition();
        }
    }

    protected Direction getDirection(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) {
            return null;
        }

        if (deltaX == 0 && deltaY == -1) {
            return Direction.UP;
        }

        if (deltaX == 0 && deltaY == 1) {
            return Direction.DOWN;
        }

        if (deltaX == -1 && deltaY == 0) {
            return Direction.LEFT;
        }

        if (deltaX == 1 && deltaY == 0) {
            return Direction.RIGHT;
        }

        return Direction.NONE;
    }

    protected TilePosition getBestAdjacentTile(Entity target) {
        TilePosition[] candidates = new TilePosition[] {
                new TilePosition(target.worldX, target.worldY - 1), // UP
                new TilePosition(target.worldX + 1, target.worldY), // RIGHT
                new TilePosition(target.worldX, target.worldY + 1), // DOWN
                new TilePosition(target.worldX - 1, target.worldY) // LEFT
        };

        TilePosition best = null;
        int bestDist = Integer.MAX_VALUE;

        for (TilePosition tile : candidates) {
            if (world.tileManager.getCollisionByXandY(tile.x, tile.y)) {
                continue;
            }

            int dist = Math.abs(tile.x - worldX) + Math.abs(tile.y - worldY);
            if (dist < bestDist) {
                bestDist = dist;
                best = tile;
            }
        }

        return best;
    }

    public void setWanderRange(int wanderRange) {
        this.wanderRange = wanderRange;
        this.wanderRangeChanged = 1;
    }

    public void setInteractionRange(int interactionRange) {
        this.interactionRange = interactionRange;
        this.interactionRangeChanged = 1;
    }

    protected void setTargetItemID(String id) {
        this.targetItemID = id;
        this.targetItemIDChanged = 1;
    }

    private void setWorldX(int x) {
        this.worldX = x;
        this.worldXChanged = 1;
    }

    private void setWorldY(int y) {
        this.worldY = y;
        this.worldYChanged = 1;
    }

    protected void setFacingDirection(Direction dir) {
        this.facingDirection = dir;
        this.facingDirectionChanged = 1;
    }

    protected void setIsDying(boolean isDying) {
        this.isDying = isDying;
        this.isDyingChanged = 1;
    }

    protected void setFollowCounter(int counter) {
        this.followCounter = counter;
        this.followCounterChanged = 1;
    }

    protected void setInteractionTargetID(String id) {
        this.interactionTargetID = id;
        this.interactionTargetIDChanged = 1;
    }

    protected void setGoalAction(GoalAction action) {
        this.goalAction = action;
        this.goalActionChanged = 1;
    }

    protected void setTargetEntityLastPosition(TilePosition position) {
        this.targetEntityLastPosition = position;
        this.targetEntityLastPositionChanged = 1;
    }

    protected void setDyingCounter(int counter) {
        this.dyingCounter = counter;
        this.dyingCounterChanged = 1;
    }

}
