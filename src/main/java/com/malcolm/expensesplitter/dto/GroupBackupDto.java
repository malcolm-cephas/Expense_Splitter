package com.malcolm.expensesplitter.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for group backup and restoration.
 * This structure is used to export all group data (members, expenses, payments, splits)
 * to a single JSON file that can be imported on another device.
 */
public class GroupBackupDto {
    public String name;
    public String description;
    public BigDecimal budget;
    public String budgetCurrency;
    public boolean familyGroupingEnabled;
    public boolean investmentEnabled;
    public List<UserBackupDto> members;
    public List<ExpenseBackupDto> expenses;
    public List<BusinessBackupDto> businesses;

    public static class BusinessBackupDto {
        public String name;
        public String description;
        public BigDecimal initialCapital;
        public List<InvestmentBackupDto> investments;
        public List<InvestmentTransactionBackupDto> transactions;
    }

    public static class InvestmentBackupDto {
        public String investorEmail;
        public BigDecimal originalInvestment;
        public BigDecimal expectedShare;
        public BigDecimal capitalReturned;
        public BigDecimal profitReceived;
    }

    public static class InvestmentTransactionBackupDto {
        public String investorEmail;
        public String type;
        public BigDecimal amount;
        public String description;
        public LocalDateTime createdAt;
    }

    public static class UserBackupDto {
        public String name;
        public String email;
        public String currencyPreference;
        public String familyName;
    }

    public static class ExpenseBackupDto {
        public String description;
        public BigDecimal amount;
        public String currency;
        public String category;
        public String paymentMode;
        public String splitType;
        public LocalDateTime createdAt;
        public LocalDate expenseDate;
        public List<PaymentBackupDto> payments;
        public List<SplitBackupDto> splits;
    }

    public static class PaymentBackupDto {
        public String userEmail;
        public BigDecimal amount;
    }

    public static class SplitBackupDto {
        public String userEmail;
        public BigDecimal owedAmount;
        public BigDecimal paidAmount;
        public boolean isPaid;
    }
}
