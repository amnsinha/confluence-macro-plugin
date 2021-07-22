package com.tngtech.confluence.plugin.data;

public class CurrentVotes {
    private int userVotes;
    private int itemVotes;

    public CurrentVotes(int userVotes, int itemVotes) {
        this.userVotes = userVotes;
        this.itemVotes = itemVotes;
    }

    public int getUserVotes() {
        return userVotes;
    }

    public int getItemVotes() {
        return itemVotes;
    }
}