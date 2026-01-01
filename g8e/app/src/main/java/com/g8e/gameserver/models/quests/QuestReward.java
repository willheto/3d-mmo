package com.g8e.gameserver.models.quests;

public class QuestReward {
    int[] skillRewards;
    int[] itemRewards;
    int influenceReward;

    public QuestReward(int[] skillRewards, int[] itemRewards, int influenceReward) {
        this.skillRewards = skillRewards;
        this.itemRewards = itemRewards;
        this.influenceReward = influenceReward;
    }

    public int[] getSkillRewards() {
        return skillRewards;
    }

    public void setSkillRewards(int[] skillRewards) {
        this.skillRewards = skillRewards;
    }

    public int[] getItemRewards() {
        return itemRewards;
    }

    public void setItemRewards(int[] itemRewards) {
        this.itemRewards = itemRewards;
    }

    public int getInfluenceReward() {
        return influenceReward;
    }

    public void setInfluenceReward(int influenceReward) {
        this.influenceReward = influenceReward;
    }

}
