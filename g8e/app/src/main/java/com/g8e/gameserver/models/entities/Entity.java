package com.g8e.gameserver.models.entities;

import java.util.List;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.enums.GoalAction;
import com.g8e.gameserver.pathfinding.AStar;
import com.g8e.gameserver.pathfinding.PathNode;
import com.g8e.gameserver.tile.Tile;
import com.g8e.gameserver.tile.TilePosition;

public abstract class Entity {
    public String entityID;
    public int worldX;
    public int worldY;
    public Direction nextTileDirection = Direction.NONE;
    public Direction facingDirection = Direction.DOWN;
    public List<PathNode> currentPath;

    public transient World world;
    public transient AStar pathFinder;

    public transient int originalWorldX; // Where entity respawns
    public transient int originalWorldY; // Where entity respawns

    public transient TilePosition targetTile = null;
    public transient TilePosition newTargetTile = null;
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

    public int entityIDChanged = 1;

    public int worldXChanged = 1;
    public int worldYChanged = 1;
    public int facingDirectionChanged = 1;

    public int targetTileChanged = 1;
    public int newTargetTileChanged = 1;
    public int nextTileDirectionChanged = 1;
    public int currentPathChanged = 1;
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

    protected void setNewTargetTileWithingWanderArea() {
        TilePosition randomPosition = new TilePosition(
                this.originalWorldX + (int) (Math.random() * (this.wanderRange * 2 + 1)
                        - this.wanderRange),

                this.originalWorldY + (int) (Math.random() * (this.wanderRange * 2 + 1)
                        - this.wanderRange));

        // Check if the new target tile is walkable
        boolean collision = this.world.tileManager.getCollisionByXandY(randomPosition.x, randomPosition.y);
        if (!collision) {
            setNewTargetTile(randomPosition);
        }
    }

    protected boolean isTargetTileNotWithinWanderArea() {
        TilePosition currentTargetTile = this.getTarget();
        if (currentTargetTile == null) {
            return false;
        }

        return Math.abs(targetTile.x - this.originalWorldX) > this.wanderRange
                || Math.abs(targetTile.y - this.originalWorldY) > this.wanderRange;

    }

    private TilePosition getPositionOneTileAwayFromTarget(TilePosition target) {
        // Create an array of tile offsets around the target
        TilePosition[] tilesAroundTarget = new TilePosition[] {
                new TilePosition(target.x, target.y - 1), // Up
                new TilePosition(target.x + 1, target.y), // Right
                new TilePosition(target.x, target.y + 1), // Down
                new TilePosition(target.x - 1, target.y), // Left
        };

        TilePosition closestTile = null;
        double minDistance = Double.MAX_VALUE;

        // Check each tile and calculate its distance from the current position
        for (TilePosition tile : tilesAroundTarget) {
            Tile currentTile = this.world.tileManager.getTileByXandY(tile.x, tile.y);

            if (currentTile != null && !currentTile.collision) {
                // Calculate Euclidean distance to the current position
                double distance = Math.sqrt(Math.pow(tile.x - this.worldX, 2) + Math.pow(tile.y - this.worldY, 2));

                if (distance < minDistance) {
                    minDistance = distance;
                    closestTile = tile;
                }
                if (distance == minDistance) {
                    // might as well pick a random one if they are the same distance
                    if (Math.random() > 0.5) {

                        minDistance = distance;
                        closestTile = tile;
                    }
                }
            }
        }

        return closestTile;
    }

    private void setAttackTargetTile() {
        Entity entity = this.world.getEntityByID(((Combatant) this).targetedEntityID);
        if (entity != null && entity instanceof Combatant) {

            TilePosition entityTile = new TilePosition(entity.worldX, entity.worldY);

            if (this.targetEntityLastPosition != null &&
                    this.targetEntityLastPosition.getX() == entityTile.getX()
                    && this.targetEntityLastPosition.getY() == entityTile.getY()) {
                return;
            }
            setTargetEntityLastPosition(entityTile);
            setNewTargetTile(getPositionOneTileAwayFromTarget(entityTile));
        } else {
            ((Combatant) this).setTargetedEntityID(null);
        }
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

    protected void moveToNextTile() {
        switch (this.nextTileDirection) {
            case UP -> move(this.worldX, this.worldY - 1);
            case DOWN -> move(this.worldX, this.worldY + 1);
            case LEFT -> move(this.worldX - 1, this.worldY);
            case RIGHT -> move(this.worldX + 1, this.worldY);
            default -> {
            }
        }
    }

    protected void moveTowardsTarget() {

        if (this.nextTileDirection != null) {
            moveToNextTile();
        }

        if (this instanceof Combatant && ((Combatant) this).targetedEntityID != null) {
            setAttackTargetTile();
        }

        TilePosition target = this.getTarget();

        if (target == null) {
            return;
        }

        if (this.newTargetTile != null) {

            currentPath = this.pathFinder.findPath(this.worldX, this.worldY, target.x, target.y);

            if (currentPath.size() < 2) {
                currentPath = null;
                stopAllMovement();
                return;
            }
            setTargetTile(newTargetTile);
            setNewTargetTile(null);

            int deltaX = currentPath.get(1).x - this.worldX;
            int deltaY = currentPath.get(1).y - this.worldY;
            setNextTileDirection(this.getDirection(deltaX, deltaY));
            setFacingDirection(nextTileDirection);
            return;
        }

        if (currentPath == null || currentPath.isEmpty()) {
            return;
        }

        // Already at target
        if (currentPath.size() == 1) {
            stopAllMovement();
            return;
        }

        // Last step
        if (currentPath.size() == 2) {
            PathNode nextStep = currentPath.get(1);
            moveAlongPath(nextStep);
            if (this.targetItemID != null && this instanceof Player) {
                ((Player) this).takeItem(targetItemID);
                setTargetItemID(null);
            }
        } else if (currentPath.size() > 2) {
            PathNode nextStep = currentPath.get(1);
            PathNode nextNextStep = currentPath.get(2);
            moveAlongPath(nextStep, nextNextStep);
        }

    }

    protected void moveAlongPath(PathNode nextStep, PathNode nextNextStep) {
        Direction direction = this.getDirection(nextNextStep.x - nextStep.x, nextNextStep.y - nextStep.y);
        setNextTileDirection(direction);
        setFacingDirection(direction);
        currentPath.remove(0);
    }

    // Last step
    protected void moveAlongPath(PathNode nextStep) {
        setNextTileDirection(Direction.NONE);
        setTargetTile(null);
        currentPath = null;
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

        return null;
    }

    protected TilePosition getTarget() {
        return this.newTargetTile != null ? this.newTargetTile : this.targetTile;
    }

    protected void stopAllMovement() {
        setNewTargetTile(null);
        setTargetTile(null);
        setNextTileDirection(Direction.NONE);
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

    protected void setTargetTile(TilePosition tile) {
        this.targetTile = tile;
        this.targetTileChanged = 1;
    }

    protected void setNextTileDirection(Direction dir) {
        this.nextTileDirection = dir;
        this.nextTileDirectionChanged = 1;
    }

    protected void setFacingDirection(Direction dir) {
        this.facingDirection = dir;
        this.facingDirectionChanged = 1;
    }

    protected void setNewTargetTile(TilePosition tile) {
        this.newTargetTile = tile;
        this.newTargetTileChanged = 1;
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

    protected void setCurrentPath(List<PathNode> path) {
        this.currentPath = path;
        this.currentPathChanged = 1;
    }
}
