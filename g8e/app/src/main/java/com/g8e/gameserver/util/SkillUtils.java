package com.g8e.gameserver.util;

import java.util.HashMap;
import java.util.Map;

public class SkillUtils {

    // Constants representing each skill
    public static final int ATTACK = 0;
    public static final int STRENGTH = 1;
    public static final int DEFENCE = 2;
    public static final int HITPOINTS = 3;
    public static final int MAGIC = 4;

    // Maps for name-to-number and number-to-name lookups
    private static final Map<String, Integer> NAME_TO_NUMBER_MAP = new HashMap<>();
    private static final Map<Integer, String> NUMBER_TO_NAME_MAP = new HashMap<>();

    // Static block to initialize the maps
    static {
        NAME_TO_NUMBER_MAP.put("attack", ATTACK);
        NAME_TO_NUMBER_MAP.put("strength", STRENGTH);
        NAME_TO_NUMBER_MAP.put("defence", DEFENCE);
        NAME_TO_NUMBER_MAP.put("hitpoints", HITPOINTS);
        NAME_TO_NUMBER_MAP.put("magic", MAGIC);

        // Populate the reverse map
        for (Map.Entry<String, Integer> entry : NAME_TO_NUMBER_MAP.entrySet()) {
            NUMBER_TO_NAME_MAP.put(entry.getValue(), entry.getKey());
        }
    }

    // Static method to get skill number by name
    public static int getSkillNumberByName(String skillName) {
        Integer skillNumber = NAME_TO_NUMBER_MAP.get(skillName.toLowerCase());
        if (skillNumber == null) {
            throw new IllegalArgumentException("Invalid skill name: " + skillName);
        }
        return skillNumber;
    }

    // Static method to get skill name by number
    public static String getSkillNameByNumber(int skillNumber) {
        String skillName = NUMBER_TO_NAME_MAP.get(skillNumber);
        if (skillName == null) {
            throw new IllegalArgumentException("Invalid skill number: " + skillNumber);
        }
        return skillName;
    }

}
