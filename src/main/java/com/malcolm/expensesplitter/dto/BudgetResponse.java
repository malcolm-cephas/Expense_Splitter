package com.malcolm.expensesplitter.dto;

import java.math.BigDecimal;

public class BudgetResponse {
    private BigDecimal budget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private double percentageUsed;

    public BudgetResponse(BigDecimal budget, BigDecimal spent) {
        this.budget = budget;
        this.spent = spent;
        this.remaining = budget.subtract(spent);
        if (budget.compareTo(BigDecimal.ZERO) > 0) {
            this.percentageUsed = spent.multiply(new BigDecimal("100")).divide(budget, 2, java.math.RoundingMode.HALF_UP).doubleValue();
        } else {
            this.percentageUsed = 0;
        }
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }
}
