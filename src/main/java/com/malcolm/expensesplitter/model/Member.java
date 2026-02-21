package com.malcolm.expensesplitter.model;

public class Member {
    private int id;
    private int groupId;
    private String name;

    public Member(int id, int groupId, String name) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
    }

    // Constructor before DB insert
    public Member(int groupId, String name) {
        this(0, groupId, name);
    }

    public int getId() {
        return id;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
