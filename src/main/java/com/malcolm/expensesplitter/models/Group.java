package com.malcolm.expensesplitter.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    private BigDecimal budget;
    private String budgetCurrency;

    @jakarta.persistence.Column(nullable = false, columnDefinition = "boolean default false")
    private boolean familyGroupingEnabled = false;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Expense> expenses = new java.util.ArrayList<>();

    public Group() {
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        expense.setGroup(this);
    }

    public void removeExpense(Expense expense) {
        expenses.remove(expense);
        expense.setGroup(null);
    }

    public Group(String name, String description, User createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getBudgetCurrency() {
        return budgetCurrency;
    }

    public void setBudgetCurrency(String budgetCurrency) {
        this.budgetCurrency = budgetCurrency;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }

    public boolean isFamilyGroupingEnabled() {
        return familyGroupingEnabled;
    }

    public void setFamilyGroupingEnabled(boolean familyGroupingEnabled) {
        this.familyGroupingEnabled = familyGroupingEnabled;
    }
}
