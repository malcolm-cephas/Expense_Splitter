package com.malcolm.expensesplitter.model;

import java.time.LocalDate;

public class Expense {
    private int id;
    private int groupId;
    private int payerId;
    private String description;
    private double amount;
    private LocalDate date;

    public Expense(int id, int groupId, int payerId,
                   String description, double amount, LocalDate date) {
        this.id = id;
        this.groupId = groupId;
        this.payerId = payerId;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    // Constructor before DB insert
    public Expense(int groupId, int payerId,
                   String description, double amount, LocalDate date) {
        this(0, groupId, payerId, description, amount, date);
    }

    public int getId() {
        return id;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getPayerId() {
        return payerId;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }
}
