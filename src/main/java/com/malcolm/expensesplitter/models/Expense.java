package com.malcolm.expensesplitter.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // Legacy field - kept as nullable to satisfy existing DB schema
    // and allow Hibernate to auto-update 'NOT NULL' constraint to Nullable
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paid_by_id", nullable = true)
    private User legacyPaidBy;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ExpensePayment> payments = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String currency;
    private String description;
    private String category;

    // Cash, UPI, Card, etc.
    private String paymentMode;

    @Enumerated(EnumType.STRING)
    private SplitType splitType;

    private String receiptPath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ExpenseSplit> splits = new ArrayList<>();

    public Expense() {
    }

    public Expense(Group group, BigDecimal amount, String description, SplitType splitType) {
        this.group = group;
        this.amount = amount;
        this.description = description;
        this.splitType = splitType;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public User getLegacyPaidBy() {
        return legacyPaidBy;
    }

    public void setLegacyPaidBy(User legacyPaidBy) {
        this.legacyPaidBy = legacyPaidBy;
    }

    public List<ExpensePayment> getPayments() {
        return payments;
    }

    public void setPayments(List<ExpensePayment> payments) {
        this.payments = payments;
    }

    public void addPayment(ExpensePayment payment) {
        payments.add(payment);
        payment.setExpense(this);
    }

    public void removePayment(ExpensePayment payment) {
        payments.remove(payment);
        payment.setExpense(null);
    }

    /**
     * Returns the primary payer (for UI or legacy support).
     * If multiple exists, returns the first one.
     */
    @Transient
    public User getPaidBy() {
        if (payments == null || payments.isEmpty())
            return null;
        return payments.get(0).getUser();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExpenseSplit> getSplits() {
        return splits;
    }

    public void setSplits(List<ExpenseSplit> splits) {
        this.splits = splits;
    }

    public void addSplit(ExpenseSplit split) {
        splits.add(split);
        split.setExpense(this);
    }

    public void removeSplit(ExpenseSplit split) {
        splits.remove(split);
        split.setExpense(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Expense expense = (Expense) o;
        return java.util.Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
