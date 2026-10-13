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

    @Autowired
    private InvestmentService investmentService;

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
        Group group = groupRepository.findById(groupId).orElseThrow();
        Map<UUID, BigDecimal> investmentBalances = investmentService.calculateNetBalances(group);
        boolean useInvestmentLedger = group.isInvestmentEnabled() && !investmentBalances.isEmpty();
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        Map<UUID, BigDecimal> balances = new HashMap<>();

        // When investment contributions/shares exist, they already represent the
        // complete album-style settlement. Adding ordinary expense balances too
        // would credit the person who paid the supplier a second time.
        if (!useInvestmentLedger) {
            for (Expense expense : expenses) {
                BigDecimal totalExpenseAmount = expense.getAmount();
                if (totalExpenseAmount == null || totalExpenseAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String expenseCurrency = expense.getCurrency();
                String baseCurrency = appConfig.getCurrencyCode();
                BigDecimal rate = BigDecimal.ONE;
                if (expenseCurrency != null && !expenseCurrency.isEmpty()
                        && !expenseCurrency.equalsIgnoreCase(baseCurrency)) {
                    rate = exchangeRateService.getExchangeRate(expenseCurrency, baseCurrency);
                }

                // A split's paidAmount is the amount that member has already paid
                // toward this expense. Preserve overpayments as positive balances
                // instead of ignoring them when paidAmount exceeds owedAmount.
                BigDecimal netOutstanding = BigDecimal.ZERO;
                for (ExpenseSplit split : expense.getSplits()) {
                    BigDecimal owed = split.getOwedAmount() == null ? BigDecimal.ZERO : split.getOwedAmount();
                    BigDecimal paid = split.getPaidAmount() == null ? BigDecimal.ZERO : split.getPaidAmount();
                    BigDecimal memberNet = paid.subtract(owed).multiply(rate);
                    UUID memberId = split.getUser().getId();
                    balances.merge(memberId, memberNet, BigDecimal::add);
                    netOutstanding = netOutstanding.add(owed.subtract(paid));
                }

                // The expense payer(s) are owed only the remaining net amount.
                // Allocate that credit by their share of the original expense.
                // If the group has collectively overpaid, netOutstanding is
                // negative, correctly making the original payer a debtor.
                BigDecimal convertedOutstanding = netOutstanding.multiply(rate);
                if (convertedOutstanding.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal totalRecordedPayments = expense.getPayments().stream()
                            .map(payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal denominator = totalRecordedPayments.compareTo(BigDecimal.ZERO) > 0
                            ? totalRecordedPayments : totalExpenseAmount;

                    if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                        for (com.malcolm.expensesplitter.models.ExpensePayment payment : expense.getPayments()) {
                            UUID payerId = payment.getUser().getId();
                            BigDecimal payerShare = (payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
                                    .divide(denominator, 8, RoundingMode.HALF_UP);
                            BigDecimal payerCredit = convertedOutstanding.multiply(payerShare);
                            balances.merge(payerId, payerCredit, BigDecimal::add);
                        }
                    }
                }
            }
        }

        if (useInvestmentLedger) {
            balances.putAll(investmentBalances);
        }
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
