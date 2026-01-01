package com.g8e.gameserver.models;

import java.util.Random;

public class DropTable {
    final private int itemID;
    final private float dropChance;
    final private int amount;

    public DropTable(int itemID, float dropChance, int amount) {
        this.itemID = itemID;
        this.dropChance = dropChance;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public int getItemID() {
        return itemID;
    }

    public float getDropChance() {
        return dropChance;
    }

    public static DropTable getRolledDrop(DropTable[] dropTables) {
        if (dropTables.length == 0) {
            return null;
        }
        Random random = new Random();

        // Step 1: Pick a random DropTable entry
        DropTable dropTable = dropTables[random.nextInt(dropTables.length)];

        // Step 2: Roll for the drop
        float roll = random.nextFloat();
        if (roll < dropTable.getDropChance()) {
            return dropTable;
        }

        return null;
    }

}
