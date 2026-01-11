package com.g8e.gameserver.network.dataTransferModels;

import com.g8e.gameserver.models.objects.Item;

public class DTOItem {
    final private String uniqueID;
    private Integer itemID;
    private Integer amount;
    private Integer worldX;
    private Integer worldY;

    public DTOItem(Item item) {
        this.uniqueID = item.getUniqueID();

        if (item.itemIDChanged == 1) {
            this.itemID = item.getItemID();
        }

        if (item.amountChanged == 1) {
            this.amount = item.getAmount();
        }

        if (item.worldXChanged == 1) {
            this.worldX = item.worldXChanged;
        }

        if (item.worldYChanged == 1) {
            this.worldY = item.worldYChanged;
        }
    }

    public DTOItem(Item item, boolean includeEverything) {
        this.uniqueID = item.getUniqueID();
        this.itemID = item.getItemID();
        this.amount = item.getAmount();
        this.worldX = item.worldXChanged;
        this.worldX = item.worldXChanged;
    }

    public boolean hasOnlyUniqueId() {
        return uniqueID != null
                && itemID == null
                && amount == null
                && worldX == null
                && worldY == null;
    }

}
