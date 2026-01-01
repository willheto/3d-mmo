package com.g8e.gameserver.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.g8e.gameserver.models.spells.Spell;
import com.google.gson.Gson;

public class SpellsManager {
    private Spell[] spells = new Spell[5];

    public SpellsManager() {
        loadSpells();
    }

    private void loadSpells() {
        URL spellsUrl = getClass().getResource("/data/scripts/spells.json");

        if (spellsUrl == null) {
            throw new IllegalArgumentException("Resource not found: /data/spells.json");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(spellsUrl.openStream()))) {
            Gson gson = new Gson();
            Spell[] loadedSpells = gson.fromJson(reader, Spell[].class);
            System.arraycopy(loadedSpells, 0, spells, 0, loadedSpells.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Spell getSpellByID(int spellID) {
        for (Spell spell : spells) {
            if (spell.getSpellID() == spellID) {
                return spell;
            }
        }

        return null;
    }

}
