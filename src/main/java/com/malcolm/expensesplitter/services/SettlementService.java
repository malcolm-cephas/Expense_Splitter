package com.malcolm.expensesplitter.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
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

    /**
     * Internal node representing a user's net balance.
     */
    private static class BalanceNode {
        UUID userId;
        BigDecimal amount;

        BalanceNode(UUID userId, BigDecimal amount) {
            this.userId = userId;
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

                balances.put(payerId, balances.getOrDefault(payerId, BigDecimal.ZERO).add(credit));
            }

            // 3. Debit members for their own *remaining* unpaid debt
            for (ExpenseSplit split : expense.getSplits()) {
                UUID debtorId = split.getUser().getId();
                BigDecimal unpaid = split.getOwedAmount().subtract(split.getPaidAmount());

                if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
                    balances.put(debtorId, balances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(unpaid));
                }
            }
        }

        // 4. Run simplification algorithm (Greedy approach)
        PriorityQueue<BalanceNode> creditors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));
        PriorityQueue<BalanceNode> debtors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));

        balances.forEach((userId, amount) -> {
            // Use 0.001 as threshold to handle tiny floating point/division dust
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

            BigDecimal settledAmount = credit.amount.min(debt.amount).setScale(2, RoundingMode.HALF_UP);
            if (settledAmount.compareTo(BigDecimal.ZERO) > 0) {
                transactions.add(new TransactionDto(debt.userId, credit.userId, settledAmount));
            }

            credit.amount = credit.amount.subtract(settledAmount);
            debt.amount = debt.amount.subtract(settledAmount);

            if (credit.amount.compareTo(new BigDecimal("0.001")) > 0)
                creditors.add(credit);
            if (debt.amount.compareTo(new BigDecimal("0.001")) > 0)
                debtors.add(debt);
        }

        return transactions;
    }
}
