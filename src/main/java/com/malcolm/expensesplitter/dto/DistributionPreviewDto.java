package com.malcolm.expensesplitter.dto;

import com.malcolm.expensesplitter.models.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DistributionPreviewDto {
    private BigDecimal distributableAmount;
    private BigDecimal totalCapitalRecovery;
    private BigDecimal totalProfit;
    private BigDecimal totalOutstandingCapital;
    private final List<DistributionLineItem> lineItems = new ArrayList<>();

    public BigDecimal getDistributableAmount() {
        return distributableAmount;
    }

    public void setDistributableAmount(BigDecimal distributableAmount) {
        this.distributableAmount = distributableAmount;
    }

    public BigDecimal getTotalCapitalRecovery() {
        return totalCapitalRecovery;
    }

    public void setTotalCapitalRecovery(BigDecimal totalCapitalRecovery) {
        this.totalCapitalRecovery = totalCapitalRecovery;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getTotalOutstandingCapital() {
        return totalOutstandingCapital;
    }

    public void setTotalOutstandingCapital(BigDecimal totalOutstandingCapital) {
        this.totalOutstandingCapital = totalOutstandingCapital;
    }

    public List<DistributionLineItem> getLineItems() {
        return lineItems;
    }

    public static class DistributionLineItem {
        private UUID investorId;
        private String investorName;
        private BigDecimal capitalRecovery;
        private BigDecimal profit;
        private BigDecimal total;

        public DistributionLineItem(User investor, BigDecimal capitalRecovery, BigDecimal profit, BigDecimal total) {
            this.investorId = investor != null ? investor.getId() : null;
            this.investorName = investor != null ? investor.getName() : null;
            this.capitalRecovery = capitalRecovery;
            this.profit = profit;
            this.total = total;
        }

        public UUID getInvestorId() {
            return investorId;
        }

        public void setInvestorId(UUID investorId) {
            this.investorId = investorId;
        }

        public String getInvestorName() {
            return investorName;
        }

        public void setInvestorName(String investorName) {
            this.investorName = investorName;
        }

        public BigDecimal getCapitalRecovery() {
            return capitalRecovery;
        }

        public void setCapitalRecovery(BigDecimal capitalRecovery) {
            this.capitalRecovery = capitalRecovery;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public void setProfit(BigDecimal profit) {
            this.profit = profit;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }
}
