package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SettlementService {

    @Autowired
    private ExpenseRepository expenseRepository;

    private static class BalanceNode {
        UUID userId;
        BigDecimal amount;

        BalanceNode(UUID userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }

    public List<TransactionDto> calculateSimplifiedDebts(UUID groupId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        Map<UUID, BigDecimal> balances = new HashMap<>();

        // 1. Calculate net balance for each user based on UNPAID splits
        for (Expense expense : expenses) {
            UUID paidBy = expense.getPaidBy().getId();

            for (ExpenseSplit split : expense.getSplits()) {
                if (!split.isPaid()) {
                    UUID debtorId = split.getUser().getId();
                    BigDecimal amount = split.getOwedAmount();

                    // Debtor owes this amount
                    balances.put(debtorId, balances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(amount));
                    // Payer is owed this amount
                    balances.put(paidBy, balances.getOrDefault(paidBy, BigDecimal.ZERO).add(amount));
                }
            }
        }

        // 2. Run simplification algorithm
        // Sort by largest amounts for greedy algorithm
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

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            BalanceNode credit = creditors.poll();
            BalanceNode debt = debtors.poll();

            BigDecimal settledAmount = credit.amount.min(debt.amount);

            transactions.add(new TransactionDto(debt.userId, credit.userId, settledAmount));

            credit.amount = credit.amount.subtract(settledAmount);
            debt.amount = debt.amount.subtract(settledAmount);

            if (credit.amount.compareTo(new BigDecimal("0.01")) > 0)
                creditors.add(credit);
            if (debt.amount.compareTo(new BigDecimal("0.01")) > 0)
                debtors.add(debt);
        }

        return transactions;
    }
}
