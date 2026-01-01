package com.g8e.gameserver.models.quests;

public class Quest {
    private int questID;
    private String name;
    private String description;
    private String start;
    private QuestRequirements requirements;
    private QuestReward rewards;
    private Steps steps;

    public Quest(int questID, String name, String description, String start, QuestRequirements questRequirements,
            QuestReward rewards, Steps steps) {
        this.questID = questID;
        this.name = name;
        this.description = description;
        this.start = start;
        this.requirements = questRequirements;
        this.rewards = rewards;
        this.steps = steps;
    }

    public Steps getSteps() {
        return steps;
    }

    public void setSteps(Steps steps) {
        this.steps = steps;
    }

    public int getQuestID() {
        return questID;
    }

    public void setQuestID(int questID) {
        this.questID = questID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestRequirements getRequirements() {
        return requirements;
    }

    public QuestReward getRewards() {
        return rewards;
    }

    public void setRewards(QuestReward rewards) {
        this.rewards = rewards;
    }

    public void setRequirements(QuestRequirements requirements) {
        this.requirements = requirements;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

}
