package com.g8e.gameserver.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.g8e.gameserver.models.quests.Quest;
import com.google.gson.Gson;

public class QuestsManager {
    private Quest[] quests = new Quest[3];

    public QuestsManager() {
        loadQuests();
    }

    private void loadQuests() {
        URL questsUrl = getClass().getResource("/data/scripts/quests.json");

        if (questsUrl == null) {
            throw new IllegalArgumentException("Resource not found: /data/quests.json");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(questsUrl.openStream()))) {
            Gson gson = new Gson();
            Quest[] loadedQuests = gson.fromJson(reader, Quest[].class);
            System.arraycopy(loadedQuests, 0, quests, 0, loadedQuests.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Quest getQuestByID(int questID) {
        for (Quest quest : quests) {
            if (quest.getQuestID() == questID) {
                return quest;
            }
        }

        return null;
    }

}
