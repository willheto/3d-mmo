package com.g8e.gameserver.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.g8e.gameserver.World;
import com.g8e.gameserver.tile.TilePosition;

public class AStar {
    private final World world;

    public AStar(World world) {
        this.world = world;
    }

    public List<PathNode> findPath(int startX, int startY, int targetX, int targetY) {
        PriorityQueue<PathNode> openList = new PriorityQueue<>(Comparator.comparingInt(a -> a.f));
        Set<PathNode> closedList = new HashSet<>();

        PathNode startPathNode = new PathNode(startX, startY, null);

        PathNode targetPathNode = new PathNode(targetX, targetY, null);

        startPathNode.g = 0;
        startPathNode.h = getDistance(startPathNode, targetPathNode);
        startPathNode.f = startPathNode.h;

        if (world.tileManager.getCollisionByXandY(targetX, targetY)) {
            // find closest walkable tile
            TilePosition closestWalkableTile = world.tileManager.getClosestWalkableTile(targetX, targetY);

            if (closestWalkableTile == null) {
                return new ArrayList<>(); // Early exit if target is unreachable
            }

            targetPathNode = new PathNode(closestWalkableTile.x, closestWalkableTile.y, null);

        }

        openList.add(startPathNode);

        while (!openList.isEmpty()) {
            PathNode currentPathNode = openList.poll();

            // Check if we've reached the target
            if (currentPathNode.equals(targetPathNode)) {
                return constructPath(currentPathNode);
            }

            closedList.add(currentPathNode);

            // Get the neighbors (up, down, left, right)
            List<PathNode> neighbors = getNeighbors(currentPathNode);

            for (PathNode neighbor : neighbors) {
                if (closedList.contains(neighbor))
                    continue;

                int gCost = currentPathNode.g + getDistance(currentPathNode, neighbor);
                boolean isInOpenList = openList.contains(neighbor);

                // If the neighbor is not in the open list, or we found a shorter path to it
                if (!isInOpenList || gCost < neighbor.g) {
                    neighbor.g = gCost;
                    neighbor.h = getDistance(neighbor, targetPathNode);
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.parent = currentPathNode;

                    if (isInOpenList) {
                        openList.remove(neighbor); // force re-heap
                    }

                    openList.add(neighbor);
                }

            }
        }

        // If no path is found, return an empty list
        return new ArrayList<>();
    }

    private List<PathNode> getNeighbors(PathNode current) {
        List<PathNode> neighbors = new ArrayList<>();

        int[][] directions = {
                { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 },
                { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }
        };

        for (int[] d : directions) {
            int nx = current.x + d[0];
            int ny = current.y + d[1];

            // target tile blocked
            if (world.tileManager.getCollisionByXandY(nx, ny)) {
                continue;
            }

            // diagonal corner check
            if (d[0] != 0 && d[1] != 0) {
                if (world.tileManager.getCollisionByXandY(current.x + d[0], current.y))
                    continue;
                if (world.tileManager.getCollisionByXandY(current.x, current.y + d[1]))
                    continue;
            }

            neighbors.add(new PathNode(nx, ny, current));
        }

        return neighbors;
    }

    private int getDistance(PathNode a, PathNode b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);

        int straight = Math.abs(dx - dy);
        int diagonal = Math.min(dx, dy);

        // scale by 10 to keep ints
        return diagonal * 14 + straight * 10;
    }

    private List<PathNode> constructPath(PathNode currentPathNode) {
        List<PathNode> path = new ArrayList<>();
        while (currentPathNode != null) {
            path.add(currentPathNode);
            currentPathNode = currentPathNode.parent;
        }
        Collections.reverse(path); // Reverse to get the path from start to target
        return path;
    }

}