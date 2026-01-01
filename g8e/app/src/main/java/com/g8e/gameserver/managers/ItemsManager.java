package com.g8e.gameserver.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import com.g8e.util.Logger;

import com.g8e.gameserver.World;
import com.g8e.gameserver.models.objects.DespawningItem;
import com.g8e.gameserver.models.objects.Edible;
import com.g8e.gameserver.models.objects.Item;
import com.g8e.gameserver.models.objects.Wieldable;
import com.google.gson.Gson;

public class ItemsManager {
    private Item[] items = new Item[1000];
    private Wieldable[] wieldables = new Wieldable[100];
    private Edible[] edibles = new Edible[100];
    private World world;
    private DespawningItem[] despawningItems = new DespawningItem[1000];

    public ItemsManager(World world) {
        loadItems();
        loadWieldables();
        loadEdibles();
        this.world = world;
    }

    private void loadItems() {
        URL itemsUrl = getClass().getResource("/data/scripts/items.json");

        if (itemsUrl == null) {
            throw new IllegalArgumentException("Resource not found: /data/items.json");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(itemsUrl.openStream()))) {
            Gson gson = new Gson();
            Item[] loadedItems = gson.fromJson(reader, Item[].class);
            System.arraycopy(loadedItems, 0, items, 0, loadedItems.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadWieldables() {
        URL wieldablesUrl = getClass().getResource("/data/scripts/wieldables.json");

        if (wieldablesUrl == null) {
            throw new IllegalArgumentException("Resource not found: /data/wieldables.json");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(wieldablesUrl.openStream()))) {
            Gson gson = new Gson();
            Wieldable[] loadedWieldables = gson.fromJson(reader, Wieldable[].class);
            System.arraycopy(loadedWieldables, 0, wieldables, 0, loadedWieldables.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadEdibles() {
        URL ediblesUrl = getClass().getResource("/data/scripts/edibles.json");

        if (ediblesUrl == null) {
            throw new IllegalArgumentException("Resource not found: /data/edibles.json");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ediblesUrl.openStream()))) {
            Gson gson = new Gson();
            Edible[] loadedEdibles = gson.fromJson(reader, Edible[].class);
            System.arraycopy(loadedEdibles, 0, edibles, 0, loadedEdibles.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Item getItemByID(int itemID) {
        for (Item item : items) {
            if (item != null && item.getItemID() == itemID) {
                return item;
            }
        }

        return null;
    }

    public Item getItemByUniqueItemID(String uniqueItemID) {
        for (Item item : world.items) {
            if (item.getUniqueID().equals(uniqueItemID)) {
                return item;
            }
        }

        return null;
    }

    public Edible getEdibleInfoByItemID(int itemID) {
        for (Edible edible : edibles) {
            if (edible != null && edible.getItemID() == itemID) {
                return edible;
            }
        }

        return null;
    }

    public void spawnItem(int x, int y, int itemID) {
        Item item = getItemByID(itemID);

        if (item == null) {
            Logger.printError("Item with ID " + itemID + " not found in items list");
            return;
        }

        Item newItem = new Item(item.getItemID(), item.getName(), item.getExamine(), item.isWieldable(),
                item.isStackable(),
                item.getSpriteName(),
                item.getValue());
        newItem.setWorldX(x);
        newItem.setWorldY(y);
        String uniqueID = "item_" + item.getName() + "_" + x + "_" + y + "_" + System.currentTimeMillis();

        newItem.setUniqueID(uniqueID);
        world.items.add(newItem);
    }

    public void spawnItem(int x, int y, int itemID, int despawnTime) {
        Item item = getItemByID(itemID);

        if (item == null) {
            Logger.printError("Item with ID " + itemID + " not found in items list");
            return;
        }

        Item newItem = new Item(item.getItemID(), item.getName(), item.getExamine(), item.isWieldable(),
                item.isStackable(),
                item.getSpriteName(),
                item.getValue());
        newItem.setWorldX(x);
        newItem.setWorldY(y);
        String uniqueID = "item_" + item.getName() + "_" + x + "_" + y + "_" + System.currentTimeMillis();

        newItem.setUniqueID(uniqueID);
        world.items.add(newItem);

        for (int i = 0; i < despawningItems.length; i++) {
            if (despawningItems[i] == null) {
                despawningItems[i] = new DespawningItem(uniqueID, despawnTime);
                break;
            }
        }
    }

    public void spawnItemWithAmount(int x, int y, int itemID, int despawnTime, int amount) {
        Item item = getItemByID(itemID);

        if (item == null) {
            Logger.printError("Item with ID " + itemID + " not found in items list");
            return;
        }

        if (item.isStackable() == false) {
            Logger.printError("Item with ID " + itemID + " is not stackable and will not be spawned with amount");
            return;
        }

        // check if same itemID is already on the ground
        for (Item groundItem : world.items) {
            if (groundItem.getItemID() == itemID && groundItem.getWorldX() == x && groundItem.getWorldY() == y) {
                groundItem.setAmount(groundItem.getAmount() + amount);

                // reset despawn timer
                for (DespawningItem despawningItem : despawningItems) {
                    if (despawningItem != null && despawningItem.getUniqueItemID().equals(groundItem.getUniqueID())) {
                        despawningItem.setDespawnTimer(despawnTime);
                        break;
                    }
                }
                return;
            }
        }

        Item newItem = new Item(item.getItemID(), item.getName(), item.getExamine(), item.isWieldable(),
                item.isStackable(),
                item.getSpriteName(),
                item.getValue());
        newItem.setWorldX(x);
        newItem.setWorldY(y);
        newItem.setAmount(amount);
        String uniqueID = "item_" + item.getName() + "_" + x + "_" + y + "_" + System.currentTimeMillis();

        newItem.setUniqueID(uniqueID);
        world.items.add(newItem);

        for (int i = 0; i < despawningItems.length; i++) {
            if (despawningItems[i] == null) {
                despawningItems[i] = new DespawningItem(uniqueID, despawnTime);
                break;
            }
        }
    }

    public void removeItem(String uniqueItemID) {
        world.items.removeIf(item -> item.getUniqueID().equals(uniqueItemID));
    }

    public Wieldable getWieldableInfoByItemID(int itemID) {
        for (Wieldable wieldable : wieldables) {
            if (wieldable != null && wieldable.getItemID() == itemID) {
                return wieldable;
            }
        }

        return null;
    }

    public void updateDespawnTimers() {
        for (int i = 0; i < despawningItems.length; i++) {
            DespawningItem despawningItem = despawningItems[i];
            if (despawningItem == null) {
                continue;
            }
            if (world.items.stream().noneMatch(item -> item.getUniqueID().equals(despawningItem.getUniqueItemID()))) {
                despawningItems[i] = null;
                continue;
            }
            despawningItem.setDespawnTimer(despawningItem.getDespawnTimer() - 1);

            if (despawningItem.getDespawnTimer() <= 0) {
                removeItem(despawningItem.getUniqueItemID());
                despawningItems[i] = null;
            }
        }
    }
}
