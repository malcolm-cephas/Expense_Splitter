package com.malcolm.expensesplitter.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionDto {
    private UUID from;
    private UUID to;
    private BigDecimal amount;
    private String fromName;
    private String toName;

    public TransactionDto(UUID from, UUID to, BigDecimal amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public TransactionDto(String fromName, String toName, BigDecimal amount) {
        this.fromName = fromName;
        this.toName = toName;
        this.amount = amount;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public UUID getFrom() {
        return from;
    }

    public void setFrom(UUID from) {
        this.from = from;
    }

    public UUID getTo() {
        return to;
    }

    public void setTo(UUID to) {
        this.to = to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
