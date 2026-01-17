package com.g8e.gameserver.tile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.g8e.gameserver.World;
import com.g8e.util.Logger;

public final class TileManager {

    final private World world;
    public Tile[] tile;
    public int[][] mapTileNumLayer1;
    public int[][] mapTileNumLayer2;
    public int[][] mapTileNumLayer3;
    public int[][] mapTileNumLayer4;
    public int chunkSize = 20;

    public TileManager(World world) {
        this.world = world;
        tile = new Tile[8000];

        // Initialize tile maps for each layer
        mapTileNumLayer1 = new int[world.maxWorldCol][world.maxWorldRow];
        mapTileNumLayer2 = new int[world.maxWorldCol][world.maxWorldRow];
        mapTileNumLayer3 = new int[world.maxWorldCol][world.maxWorldRow];
        mapTileNumLayer4 = new int[world.maxWorldCol][world.maxWorldRow];

        getTiles();
        loadMap("/data/map/map_layer1.csv", 1); // Load layer 1 map
        loadMap("/data/map/map_layer2.csv", 2); // Load layer 2 map
        loadMap("/data/map/map_objects.csv", 3); // Load layer 2 map

    }

    public TilePosition getClosestWalkableTile(int x, int y) {
        int distance = 0;

        while (true) {
            // Search in increasing distance from the target tile
            for (int i = -distance; i <= distance; i++) {
                for (int j = -distance; j <= distance; j++) {
                    // Only check the outermost tiles of the current square (Manhattan distance)
                    if (i == -distance || i == distance || j == -distance || j == distance) {
                        int newX = x + i;
                        int newY = y + j;

                        // Check bounds to avoid IndexOutOfBoundsException
                        if (newX >= 0 && newX < world.maxWorldCol && newY >= 0 && newY < world.maxWorldRow) {
                            if (!getCollisionByXandY(newX, newY)) {
                                return new TilePosition(newX, newY);
                            }
                        }
                    }
                }
            }
            // If no walkable tile found, expand the search area by increasing the distance
            distance++;
        }

    }

    public int getChunkByWorldXandY(int worldX, int worldY) {
        try {
            // world is divided into chunks of 10x10 tiles
            // starting from top left corner of the world
            int chunkX = worldX / chunkSize;
            int chunkY = worldY / chunkSize;
            return chunkX + chunkY * (world.maxWorldCol / chunkSize);
        } catch (Exception e) {
            Logger.printError(e.getMessage());
        }
        return -1;
    }

    public int[] getNeighborChunks(int chunk) {
        try {
            int[] neighbors = new int[8];
            int chunkX = chunk % (world.maxWorldCol / chunkSize);
            int chunkY = chunk / (world.maxWorldCol / chunkSize);

            neighbors[0] = chunkX - 1 + (chunkY - 1) * (world.maxWorldCol / chunkSize);
            neighbors[1] = chunkX + (chunkY - 1) * (world.maxWorldCol / chunkSize);
            neighbors[2] = chunkX + 1 + (chunkY - 1) * (world.maxWorldCol / chunkSize);
            neighbors[3] = chunkX - 1 + chunkY * (world.maxWorldCol / chunkSize);
            neighbors[4] = chunkX + 1 + chunkY * (world.maxWorldCol / chunkSize);
            neighbors[5] = chunkX - 1 + (chunkY + 1) * (world.maxWorldCol / chunkSize);
            neighbors[6] = chunkX + (chunkY + 1) * (world.maxWorldCol / chunkSize);
            neighbors[7] = chunkX + 1 + (chunkY + 1) * (world.maxWorldCol / chunkSize);

            return neighbors;
        } catch (Exception e) {
            Logger.printError(e.getMessage());
        }
        return null;
    }

    // Is this even needed anymore?
    public Tile getTileByXandY(int x, int y) {
        try {
            // For now, return the tile from Layer 1
            int index = mapTileNumLayer1[x][y];
            if (index == -1) {
                index = mapTileNumLayer2[x][y];
            }
            return tile[index];
        } catch (Exception e) {
            Logger.printError(e.getMessage());
        }
        return null;
    }

    public boolean getCollisionByXandY(int x, int y) {

        if (x < 0 || y < 0
                || x >= mapTileNumLayer1.length
                || y >= mapTileNumLayer1[0].length) {
            return true;
        }

        int[] layers = {
                mapTileNumLayer3[x][y],
                mapTileNumLayer2[x][y],
                mapTileNumLayer1[x][y]
        };

        for (int tileIndex : layers) {
            if (tileIndex == -1)
                continue;

            return tile[tileIndex].collision;
        }

        return false;
    }

    public void getTiles() {
        tile[0] = new Tile(false, 0);
        tile[1] = new Tile(true, 1);
        tile[2] = new Tile(true, 2);
        tile[3] = new Tile(false, 3);
        tile[4] = new Tile(true, -1);

    }

    public void setup(int index, boolean collision) {
        try {
            tile[index] = new Tile(collision, index);
        } catch (Exception e) {
            Logger.printError(e.getMessage());
        }
    }

    // Method to load maps for different layers
    public void loadMap(String filePath, int layer) {
        try {
            InputStream is = getClass().getResourceAsStream(filePath);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                int col = 0;
                int row = 0;

                while (col < world.maxWorldCol && row < world.maxWorldRow) {
                    String line = br.readLine();

                    while (col < world.maxWorldCol) {
                        String numbers[] = line.split(",");
                        int num = Integer.parseInt(numbers[col]);

                        // Depending on the layer, assign the number to the respective map
                        switch (layer) {
                            case 1:
                                mapTileNumLayer1[col][row] = num;
                                break;
                            case 2:
                                mapTileNumLayer2[col][row] = num;
                                break;
                            case 3:
                                mapTileNumLayer3[col][row] = num;
                                break;
                            case 4:
                                mapTileNumLayer4[col][row] = num;
                                break;
                            default:
                                break;
                        }
                        col++;
                    }
                    if (col == world.maxWorldCol) {
                        col = 0;
                        row++;
                    }
                }
            }

        } catch (IOException | NumberFormatException e) {
            Logger.printError(e.getMessage());
        }
    }

}
