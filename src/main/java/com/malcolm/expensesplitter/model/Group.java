package com.malcolm.expensesplitter.model;

public class Group {
    private int id;
    private String name;

    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Constructor before DB insert (id not known yet)
    public Group(String name) {
        this(0, name);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // So ComboBox shows just the name
    @Override
    public String toString() {
        return name;
    }
}
