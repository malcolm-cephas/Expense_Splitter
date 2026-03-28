package com.malcolm.expensesplitter.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import com.malcolm.expensesplitter.repositories.GroupRepository;
import com.malcolm.expensesplitter.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for calculating and simplifying debts within a group.
 * Uses a greedy algorithm to minimize the number of transactions required
 * to settle all outstanding balances.
 */
@Service
public class SettlementService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private com.malcolm.expensesplitter.config.AppConfig appConfig;

    /**
     * Internal node representing a user's net balance.
     */
    private static class BalanceNode {
        UUID userId;
        String name;
        BigDecimal amount;

        BalanceNode(UUID userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        BalanceNode(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    /**
     * Calculates the minimum number of transactions needed to settle all debts in a
     * group.
     * 
     * @param groupId The unique identifier of the group.
     * @return A list of TransactionDto representing the simplified transactions.
     */
    public List<TransactionDto> calculateSimplifiedDebts(UUID groupId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        Map<UUID, BigDecimal> balances = new HashMap<>();

        for (Expense expense : expenses) {
            BigDecimal totalExpenseAmount = expense.getAmount();
            if (totalExpenseAmount.compareTo(BigDecimal.ZERO) <= 0)
                continue;

            String expenseCurrency = expense.getCurrency();
            String baseCurrency = appConfig.getCurrencyCode();
            BigDecimal rate = BigDecimal.ONE;
            if (expenseCurrency != null && !expenseCurrency.isEmpty()
                    && !expenseCurrency.equalsIgnoreCase(baseCurrency)) {
                rate = exchangeRateService.getExchangeRate(expenseCurrency, baseCurrency);
            }

            // 1. Calculate how much of this expense is still unpaid across all members
            BigDecimal totalUnpaidInExpense = BigDecimal.ZERO;
            for (ExpenseSplit split : expense.getSplits()) {
                BigDecimal unpaid = split.getOwedAmount().subtract(split.getPaidAmount());
                if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
                    totalUnpaidInExpense = totalUnpaidInExpense.add(unpaid);
                }
            }

            // 2. Credit payers for their share of the *remaining* unpaid debt
            // If the expense is 100, and 50 is unpaid, a payer who paid 100 gets 50 credit.
            for (com.malcolm.expensesplitter.models.ExpensePayment payment : expense.getPayments()) {
                UUID payerId = payment.getUser().getId();
                BigDecimal payerContribution = payment.getAmount();

                // Payer's share of the remaining group credit
                BigDecimal credit = totalUnpaidInExpense.multiply(payerContribution)
                        .divide(totalExpenseAmount, 4, RoundingMode.HALF_UP);

                credit = credit.multiply(rate);

                balances.put(payerId, balances.getOrDefault(payerId, BigDecimal.ZERO).add(credit));
            }

            // 3. Debit members for their own *remaining* unpaid debt
            for (ExpenseSplit split : expense.getSplits()) {
                UUID debtorId = split.getUser().getId();
                BigDecimal unpaid = split.getOwedAmount().subtract(split.getPaidAmount());

                if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
                    unpaid = unpaid.multiply(rate);
                    balances.put(debtorId, balances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(unpaid));
                }
            }
        }

        Group group = groupRepository.findById(groupId).orElseThrow();
        if (group.isFamilyGroupingEnabled()) {
            Map<String, BigDecimal> familyBalances = new HashMap<>();
            
            // Map individual balances to family balances
            for (com.malcolm.expensesplitter.models.User member : group.getMembers()) {
                String familyKey = member.getFamilyName();
                if (familyKey == null || familyKey.trim().isEmpty()) {
                    familyKey = member.getName(); // Fallback to individual
                }
                
                BigDecimal individualBalance = balances.getOrDefault(member.getId(), BigDecimal.ZERO);
                familyBalances.put(familyKey, familyBalances.getOrDefault(familyKey, BigDecimal.ZERO).add(individualBalance));
            }
            
            return runSimplificationAlgorithmForFamilies(familyBalances);
        }

        return runSimplificationAlgorithmForIndividuals(balances);
    }

    private List<TransactionDto> runSimplificationAlgorithmForIndividuals(Map<UUID, BigDecimal> balances) {
        // 4. Run simplification algorithm (Greedy approach)
        PriorityQueue<BalanceNode> creditors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));
        PriorityQueue<BalanceNode> debtors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));

        balances.forEach((userId, amount) -> {
            if (amount.compareTo(new BigDecimal("0.001")) > 0) {
                creditors.add(new BalanceNode(userId, amount));
            } else if (amount.compareTo(new BigDecimal("-0.001")) < 0) {
                debtors.add(new BalanceNode(userId, amount.abs()));
            }
        });

        List<TransactionDto> transactions = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            BalanceNode credit = creditors.poll();
            BalanceNode debt = debtors.poll();

            BigDecimal settledAmount = credit.amount.min(debt.amount);
            if (settledAmount.compareTo(BigDecimal.ZERO) > 0) {
                String creditorName = userRepository.findById(credit.userId).map(User::getName).orElse("Unknown");
                String debtorName = userRepository.findById(debt.userId).map(User::getName).orElse("Unknown");
                TransactionDto t = new TransactionDto(debt.userId, credit.userId, settledAmount.setScale(2, RoundingMode.HALF_UP));
                t.setFromName(debtorName);
                t.setToName(creditorName);
                transactions.add(t);
            }

            credit.amount = credit.amount.subtract(settledAmount);
            debt.amount = debt.amount.subtract(settledAmount);

            if (credit.amount.compareTo(new BigDecimal("0.001")) > 0) creditors.add(credit);
            if (debt.amount.compareTo(new BigDecimal("0.001")) > 0) debtors.add(debt);
        }
        return transactions;
    }

    private List<TransactionDto> runSimplificationAlgorithmForFamilies(Map<String, BigDecimal> balances) {
        PriorityQueue<BalanceNode> creditors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));
        PriorityQueue<BalanceNode> debtors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));

        balances.forEach((familyName, amount) -> {
            if (amount.compareTo(new BigDecimal("0.001")) > 0) {
                creditors.add(new BalanceNode(familyName, amount));
            } else if (amount.compareTo(new BigDecimal("-0.001")) < 0) {
                debtors.add(new BalanceNode(familyName, amount.abs()));
            }
        });

        List<TransactionDto> transactions = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            BalanceNode credit = creditors.poll();
            BalanceNode debt = debtors.poll();

            BigDecimal settledAmount = credit.amount.min(debt.amount);
            if (settledAmount.compareTo(BigDecimal.ZERO) > 0) {
                transactions.add(new TransactionDto(debt.name, credit.name, settledAmount.setScale(2, RoundingMode.HALF_UP)));
            }

            credit.amount = credit.amount.subtract(settledAmount);
            debt.amount = debt.amount.subtract(settledAmount);

            if (credit.amount.compareTo(new BigDecimal("0.001")) > 0) creditors.add(credit);
            if (debt.amount.compareTo(new BigDecimal("0.001")) > 0) debtors.add(debt);
        }
        return transactions;
    }

    /**
     * Marks all unpaid splits for a specific user in a group as paid.
     */
    @org.springframework.transaction.annotation.Transactional
    public void settleAllUserDebts(UUID groupId, UUID userId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        for (Expense expense : expenses) {
            boolean modified = false;
            for (ExpenseSplit split : expense.getSplits()) {
                if (split.getUser().getId().equals(userId) && !split.isPaid()) {
                    split.setPaidAmount(split.getOwedAmount());
                    split.setPaid(true); // Fixed: setPaid instead of setIsPaid
                    modified = true;
                }
            }
            if (modified) {
                expenseRepository.save(expense);
            }
        }
    }

}
