package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.models.*;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import com.malcolm.expensesplitter.repositories.GroupRepository;
import com.malcolm.expensesplitter.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for managing expenses and their splits.
 * Handles equal and unequal splits, payment status updates, and rounding
 * reconciliation.
 */
@Service
@Transactional
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryDetectionService categoryDetectionService;

    public Expense addEqualExpense(UUID groupId, UUID paidById, BigDecimal amount, String description,
            String paymentMode, String category, LocalDate expenseDate, Set<UUID> involvedMemberIds) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User paidBy = userRepository.findById(paidById).orElseThrow();

        Expense expense = new Expense(group, amount, description, SplitType.EQUAL);
        expense.addPayment(new ExpensePayment(paidBy, amount));
        expense.setPaymentMode(paymentMode);
        if (category == null || category.isEmpty() || category.equals("Other")) {
            expense.setCategory(categoryDetectionService.detectCategory(description));
        } else {
            expense.setCategory(category);
        }
        if (expenseDate != null)
            expense.setExpenseDate(expenseDate);

        Set<User> members = group.getMembers();
        Set<User> involvedMembers = members.stream()
                .filter(m -> involvedMemberIds == null || involvedMemberIds.isEmpty()
                        || involvedMemberIds.contains(m.getId()))
                .collect(Collectors.toSet());

        if (involvedMembers.isEmpty()) {
            throw new IllegalStateException("Cannot add expense with no involved members.");
        }

        BigDecimal splitAmount = amount.divide(new BigDecimal(involvedMembers.size()), 2, RoundingMode.HALF_UP);

        for (User member : involvedMembers) {
            ExpenseSplit split = new ExpenseSplit(member, splitAmount);
            expense.addSplit(split);
        }

        return expenseRepository.save(expense);
    }

    public Expense updateEqualExpense(UUID expenseId, UUID groupId, UUID paidById, BigDecimal amount,
            String description, String paymentMode, String category, LocalDate expenseDate,
            Set<UUID> involvedMemberIds) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();

        Group group = groupRepository.findById(groupId).orElseThrow();
        User paidBy = userRepository.findById(paidById).orElseThrow();

        expense.getPayments().clear();
        expense.addPayment(new ExpensePayment(paidBy, amount));
        expense.setLegacyPaidBy(paidBy);
        expense.setAmount(amount);
        expense.setDescription(description);
        expense.setPaymentMode(paymentMode);
        expense.setCategory(category);

        // Clear existing splits
        expense.getSplits().clear();

        Set<User> members = group.getMembers();
        Set<User> involvedMembers = members.stream()
                .filter(m -> involvedMemberIds == null || involvedMemberIds.isEmpty()
                        || involvedMemberIds.contains(m.getId()))
                .collect(Collectors.toSet());

        if (involvedMembers.isEmpty()) {
            throw new IllegalStateException("Cannot update expense with no involved members.");
        }

        BigDecimal splitAmount = amount.divide(new BigDecimal(involvedMembers.size()), 2, RoundingMode.HALF_UP);

        for (User member : involvedMembers) {
            ExpenseSplit split = new ExpenseSplit(member, splitAmount);
            expense.addSplit(split);
        }

        return expenseRepository.save(expense);
    }

    public Expense addExpense(UUID groupId, Map<UUID, BigDecimal> paymentInputs, BigDecimal amount, String description,
            String paymentMode, String category, LocalDate expenseDate, SplitType splitType,
            Map<UUID, BigDecimal> splitInputs, String currency) {
        Group group = groupRepository.findById(groupId).orElseThrow();

        Expense expense = new Expense(group, amount, description, splitType);
        expense.setPaymentMode(paymentMode);
        expense.setCategory(category);
        expense.setCurrency(currency);
        if (expenseDate != null)
            expense.setExpenseDate(expenseDate);

        // Add payments
        if (!paymentInputs.isEmpty()) {
            UUID firstPayerId = paymentInputs.keySet().iterator().next();
            User firstPayer = userRepository.findById(firstPayerId).orElseThrow();
            expense.setLegacyPaidBy(firstPayer);
        }

        for (Map.Entry<UUID, BigDecimal> entry : paymentInputs.entrySet()) {
            User user = userRepository.findById(entry.getKey()).orElseThrow();
            expense.addPayment(new ExpensePayment(user, entry.getValue()));
        }

        // Feature 2: Smart Category Detection
        if (expense.getCategory() == null || expense.getCategory().isEmpty() || expense.getCategory().equals("Other")) {
            expense.setCategory(categoryDetectionService.detectCategory(description));
        }

        calculateAndAddSplits(expense, splitType, splitInputs, group.getMembers());

        return expenseRepository.save(expense);
    }

    // For legacy/simple calls with single payer
    public Expense addExpense(UUID groupId, UUID paidById, BigDecimal amount, String description,
            String paymentMode, String category, LocalDate expenseDate, SplitType splitType,
            Map<UUID, BigDecimal> splitInputs, String currency) {
        Map<UUID, BigDecimal> paymentInputs = java.util.Collections.singletonMap(paidById, amount);
        return addExpense(groupId, paymentInputs, amount, description, paymentMode, category, expenseDate, splitType,
                splitInputs, currency);
    }

    public Expense updateExpense(UUID expenseId, UUID groupId, Map<UUID, BigDecimal> paymentInputs, BigDecimal amount,
            String description, String paymentMode, String category, LocalDate expenseDate, SplitType splitType,
            Map<UUID, BigDecimal> splitInputs, String currency) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();
        Group group = groupRepository.findById(groupId).orElseThrow();

        expense.setAmount(amount);
        expense.setDescription(description);
        expense.setPaymentMode(paymentMode);
        expense.setCategory(category);
        expense.setSplitType(splitType);
        expense.setCurrency(currency);
        if (expenseDate != null)
            expense.setExpenseDate(expenseDate);

        // Clear and Add new payments
        expense.getPayments().clear();
        if (!paymentInputs.isEmpty()) {
            UUID firstPayerId = paymentInputs.keySet().iterator().next();
            User firstPayer = userRepository.findById(firstPayerId).orElseThrow();
            expense.setLegacyPaidBy(firstPayer);
        }

        for (Map.Entry<UUID, BigDecimal> entry : paymentInputs.entrySet()) {
            User user = userRepository.findById(entry.getKey()).orElseThrow();
            expense.addPayment(new ExpensePayment(user, entry.getValue()));
        }

        // Clear existing splits
        expense.getSplits().clear();

        calculateAndAddSplits(expense, splitType, splitInputs, group.getMembers());

        return expenseRepository.save(expense);
    }

    // For legacy/simple calls with single payer
    public Expense updateExpense(UUID expenseId, UUID groupId, UUID paidById, BigDecimal amount,
            String description, String paymentMode, String category, LocalDate expenseDate, SplitType splitType,
            Map<UUID, BigDecimal> splitInputs, String currency) {
        Map<UUID, BigDecimal> paymentInputs = java.util.Collections.singletonMap(paidById, amount);
        return updateExpense(expenseId, groupId, paymentInputs, amount, description, paymentMode, category, expenseDate,
                splitType, splitInputs, currency);
    }

    private void calculateAndAddSplits(Expense expense, SplitType splitType, Map<UUID, BigDecimal> splitInputs,
            Set<User> allMembers) {
        BigDecimal totalAmount = expense.getAmount();

        if (splitType == SplitType.EQUAL) {
            Set<User> involved = allMembers.stream()
                    .filter(m -> splitInputs.containsKey(m.getId()))
                    .collect(Collectors.toSet());

            if (involved.isEmpty())
                throw new IllegalStateException("No members selected for equal split.");

            BigDecimal splitAmount = totalAmount.divide(new BigDecimal(involved.size()), 2, RoundingMode.HALF_UP);
            for (User member : involved) {
                expense.addSplit(new ExpenseSplit(member, splitAmount));
            }
        } else if (splitType == SplitType.EXACT) {
            for (User member : allMembers) {
                BigDecimal share = splitInputs.getOrDefault(member.getId(), BigDecimal.ZERO).setScale(2,
                        RoundingMode.HALF_UP);
                if (share.compareTo(BigDecimal.ZERO) > 0) {
                    expense.addSplit(new ExpenseSplit(member, share));
                }
            }
        } else if (splitType == SplitType.PERCENTAGE) {
            for (User member : allMembers) {
                BigDecimal percent = splitInputs.getOrDefault(member.getId(), BigDecimal.ZERO);
                if (percent.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal share = totalAmount.multiply(percent).divide(new BigDecimal("100"), 2,
                            RoundingMode.HALF_UP);
                    expense.addSplit(new ExpenseSplit(member, share));
                }
            }
        } else if (splitType == SplitType.SHARES) {
            BigDecimal totalShares = splitInputs.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalShares.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalStateException("Total shares must be greater than zero.");

            BigDecimal shareValue = totalAmount.divide(totalShares, 5, RoundingMode.HALF_UP);
            for (User member : allMembers) {
                BigDecimal memberShares = splitInputs.getOrDefault(member.getId(), BigDecimal.ZERO);
                if (memberShares.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal share = shareValue.multiply(memberShares).setScale(2, RoundingMode.HALF_UP);
                    expense.addSplit(new ExpenseSplit(member, share));
                }
            }
        }

        // Automatically mark the payers' splits as settled/paid (up to their paid
        // amount)
        // We removed the auto-mark logic because it was causing double-counting in
        // settlements.
        // The who-paid info now lives in ExpensePayment, and split.paidAmount is for
        // manual settlements only.

        // Reconcile rounding differences (ensure sum of splits matches the total amount
        // exactly)
        // This is crucial to avoid "ghost money" appearing or disappearing in the group
        // balance.
        BigDecimal totalCalculatedSplits = expense.getSplits().stream()
                .map(ExpenseSplit::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = totalAmount.subtract(totalCalculatedSplits);

        if (diff.compareTo(BigDecimal.ZERO) != 0 && !expense.getSplits().isEmpty()) {
            // Adjust the first payer's split by the difference to make it sum up correctly
            boolean adjusted = false;
            UUID primaryPayerId = expense.getPaidBy() != null ? expense.getPaidBy().getId() : null;
            if (primaryPayerId != null) {
                for (ExpenseSplit split : expense.getSplits()) {
                    if (split.getUser().getId().equals(primaryPayerId)) {
                        split.setOwedAmount(split.getOwedAmount().add(diff));
                        adjusted = true;
                        break;
                    }
                }
            }
            // If the payer is not part of the splits (e.g., they paid for others but not
            // themselves),
            // adjust the first available split.
            if (!adjusted) {
                expense.getSplits().get(0).setOwedAmount(expense.getSplits().get(0).getOwedAmount().add(diff));
            }
        }
    }

    public List<Expense> getGroupExpenses(UUID groupId) {
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    public void deleteExpense(UUID expenseId) {
        expenseRepository.deleteById(expenseId);
    }

    public void markSplitAsPaid(UUID expenseId, UUID splitId, boolean isPaid) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();
        for (ExpenseSplit split : expense.getSplits()) {
            if (split.getId().equals(splitId)) {
                split.setPaid(isPaid);
                break;
            }
        }
        expenseRepository.save(expense);
    }

    public void updateSplitPayment(UUID expenseId, UUID splitId, BigDecimal paidAmount) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();
        for (ExpenseSplit split : expense.getSplits()) {
            if (split.getId().equals(splitId)) {
                split.setPaidAmount(paidAmount);
                break;
            }
        }
        expenseRepository.save(expense);
    }

    public List<Expense> findPotentialDuplicates(UUID userId, BigDecimal amount, String description) {
        // Find expenses in the last 5 minutes
        java.time.LocalDateTime fiveMinutesAgo = java.time.LocalDateTime.now().minusMinutes(5);
        List<Expense> groupExpenses = expenseRepository.findAll(); // This is inefficient, but without a better repo method for now...

        return groupExpenses.stream()
                .filter(e -> e.getCreatedAt().isAfter(fiveMinutesAgo))
                .filter(e -> e.getAmount().compareTo(amount) == 0)
                .filter(e -> e.getDescription().equalsIgnoreCase(description))
                .filter(e -> e.getPayments().stream().anyMatch(p -> p.getUser().getId().equals(userId)))
                .collect(Collectors.toList());
    }
}
