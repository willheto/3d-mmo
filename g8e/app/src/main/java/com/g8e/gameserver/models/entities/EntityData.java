package com.g8e.gameserver.models.entities;

import com.g8e.gameserver.models.DropTable;

public class EntityData {
    int entityIndex;
    String name = "";
    String examine = "";
    int respawnTime = 0;
    int[] skills = new int[5];
    int type = 0;
    DropTable[] dropTable;

    public EntityData(int entityIndex, String name, String examine, int respawnTime, int[] skills, int type,
            String spriteName, DropTable[] dropTable) {
        this.entityIndex = entityIndex;
        this.name = name;
        this.examine = examine;
        this.respawnTime = respawnTime;
        this.skills = skills;
        this.type = type;
        this.dropTable = dropTable;
    }

    public int getEntityIndex() {
        return entityIndex;
    }

    public String getName() {
        return name;
    }

    public String getExamine() {
        return examine;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    public int[] getSkills() {
        return skills;
    }

    public int getType() {
        return type;
    }

}
