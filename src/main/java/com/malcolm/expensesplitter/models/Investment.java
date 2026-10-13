package com.malcolm.expensesplitter.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "investor_id", nullable = false)
    private User investor;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal originalInvestment = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal expectedShare = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal capitalReturned = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal profitReceived = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Investment() {
    }

    public Investment(Business business, User investor, BigDecimal originalInvestment) {
        this.business = business;
        this.investor = investor;
        this.originalInvestment = originalInvestment == null ? BigDecimal.ZERO : originalInvestment;
        this.capitalReturned = BigDecimal.ZERO;
        this.profitReceived = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public User getInvestor() {
        return investor;
    }

    public void setInvestor(User investor) {
        this.investor = investor;
    }

    public BigDecimal getOriginalInvestment() {
        return originalInvestment;
    }

    public void setOriginalInvestment(BigDecimal originalInvestment) {
        this.originalInvestment = originalInvestment == null ? BigDecimal.ZERO : originalInvestment;
    }

    public BigDecimal getExpectedShare() {
        return expectedShare == null ? BigDecimal.ZERO : expectedShare;
    }

    public void setExpectedShare(BigDecimal expectedShare) {
        this.expectedShare = expectedShare == null ? BigDecimal.ZERO : expectedShare;
    }

    public BigDecimal getCapitalReturned() {
        if (business != null && business.getTransactions() != null && !business.getTransactions().isEmpty()) {
            return business.getTransactions().stream()
                    .filter(transaction -> transaction != null && transaction.getInvestor() != null)
                    .filter(transaction -> transaction.getInvestor().getId().equals(investor != null ? investor.getId() : null))
                    .filter(transaction -> transaction.getType() == InvestmentTransactionType.CAPITAL_RETURN)
                    .map(InvestmentTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return capitalReturned == null ? BigDecimal.ZERO : capitalReturned;
    }

    public void setCapitalReturned(BigDecimal capitalReturned) {
        this.capitalReturned = capitalReturned == null ? BigDecimal.ZERO : capitalReturned;
    }

    public BigDecimal getProfitReceived() {
        if (business != null && business.getTransactions() != null && !business.getTransactions().isEmpty()) {
            return business.getTransactions().stream()
                    .filter(transaction -> transaction != null && transaction.getInvestor() != null)
                    .filter(transaction -> transaction.getInvestor().getId().equals(investor != null ? investor.getId() : null))
                    .filter(transaction -> transaction.getType() == InvestmentTransactionType.PROFIT_DISTRIBUTION)
                    .map(InvestmentTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return profitReceived == null ? BigDecimal.ZERO : profitReceived;
    }

    public void setProfitReceived(BigDecimal profitReceived) {
        this.profitReceived = profitReceived == null ? BigDecimal.ZERO : profitReceived;
    }

    public BigDecimal getOutstandingCapital() {
        return originalInvestment.subtract(getCapitalReturned());
    }

    public BigDecimal getTotalReceived() {
        return getCapitalReturned().add(getProfitReceived());
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
