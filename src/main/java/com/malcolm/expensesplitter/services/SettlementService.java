package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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

        // 1. Calculate net balance for each user based on UNPAID splits
        // Payer gets positive balance (money owed to them)
        // Debtors get negative balance (money they owe)
        for (Expense expense : expenses) {
            UUID paidBy = expense.getPaidBy().getId();

            for (ExpenseSplit split : expense.getSplits()) {
                BigDecimal remaining = split.getOwedAmount().subtract(split.getPaidAmount());
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    UUID debtorId = split.getUser().getId();

                    // Debtor owes this remaining amount (decrease balance)
                    balances.put(debtorId, balances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(remaining));
                    // Payer is still owed this remaining amount (increase balance)
                    balances.put(paidBy, balances.getOrDefault(paidBy, BigDecimal.ZERO).add(remaining));
                }
            }
        }

        // 2. Run simplification algorithm
        // We use two PriorityQueues to always pick the largest creditor and largest
        // debtor
        PriorityQueue<BalanceNode> creditors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));
        PriorityQueue<BalanceNode> debtors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));

        balances.forEach((userId, amount) -> {
            if (amount.compareTo(new BigDecimal("0.01")) > 0) { // Tolerate small rounding dust
                creditors.add(new BalanceNode(userId, amount));
            } else if (amount.compareTo(new BigDecimal("-0.01")) < 0) {
                debtors.add(new BalanceNode(userId, amount.abs()));
            }
        });

        List<TransactionDto> transactions = new ArrayList<>();

        // Greedy approach: Match the largest creditor with the largest debtor
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            BalanceNode credit = creditors.poll();
            BalanceNode debt = debtors.poll();

            // The amount to settle is the minimum of what's owed vs what's needed
            BigDecimal settledAmount = credit.amount.min(debt.amount);

            transactions.add(new TransactionDto(debt.userId, credit.userId, settledAmount));

            // Subtract the settled amount from both nodes
            credit.amount = credit.amount.subtract(settledAmount);
            debt.amount = debt.amount.subtract(settledAmount);

            // If there's still a balance remaining, add them back to the queue
            if (credit.amount.compareTo(new BigDecimal("0.01")) > 0)
                creditors.add(credit);
            if (debt.amount.compareTo(new BigDecimal("0.01")) > 0)
                debtors.add(debt);
        }

        return transactions;
    }
}
