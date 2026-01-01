package com.g8e.gameserver.models.quests;

public class QuestRequirements {
    private int[] skillRequirements;
    private int influenceRequirement;
    private String otherRequirements;

    public QuestRequirements(int[] skillRequirements, int[] itemRequirements, int influenceRequirement,
            String otherRequirements) {
        this.skillRequirements = skillRequirements;
        this.influenceRequirement = influenceRequirement;
        this.otherRequirements = otherRequirements;
    }

    public int[] getSkillRequirements() {
        return skillRequirements;
    }

    public void setSkillRequirements(int[] skillRequirements) {
        this.skillRequirements = skillRequirements;
    }

    public int getInfluenceRequirement() {
        return influenceRequirement;
    }

    public void setInfluenceRequirement(int influenceRequirement) {
        this.influenceRequirement = influenceRequirement;
    }

    public String getOtherRequirements() {
        return otherRequirements;
    }

    public void setOtherRequirements(String otherRequirements) {
        this.otherRequirements = otherRequirements;
    }

}
