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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    public Expense addEqualExpense(UUID groupId, UUID paidById, BigDecimal amount, String description,
            String paymentMode, Set<UUID> involvedMemberIds) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User paidBy = userRepository.findById(paidById).orElseThrow();

        Expense expense = new Expense(group, paidBy, amount, description, SplitType.EQUAL);
        expense.setPaymentMode(paymentMode);

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
            String description,
            String paymentMode, Set<UUID> involvedMemberIds) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();

        Group group = groupRepository.findById(groupId).orElseThrow();
        User paidBy = userRepository.findById(paidById).orElseThrow();

        expense.setPaidBy(paidBy);
        expense.setAmount(amount);
        expense.setDescription(description);
        expense.setPaymentMode(paymentMode);

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

    public Expense addExpense(UUID groupId, UUID paidById, BigDecimal amount, String description,
            String paymentMode, SplitType splitType, Map<UUID, BigDecimal> splitInputs) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User paidBy = userRepository.findById(paidById).orElseThrow();

        Expense expense = new Expense(group, paidBy, amount, description, splitType);
        expense.setPaymentMode(paymentMode);

        calculateAndAddSplits(expense, splitType, splitInputs, group.getMembers());

        return expenseRepository.save(expense);
    }

    public Expense updateExpense(UUID expenseId, UUID groupId, UUID paidById, BigDecimal amount,
            String description, String paymentMode, SplitType splitType, Map<UUID, BigDecimal> splitInputs) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow();
        Group group = groupRepository.findById(groupId).orElseThrow();
        User paidBy = userRepository.findById(paidById).orElseThrow();

        expense.setPaidBy(paidBy);
        expense.setAmount(amount);
        expense.setDescription(description);
        expense.setPaymentMode(paymentMode);
        expense.setSplitType(splitType);

        // Clear existing splits
        expense.getSplits().clear();

        calculateAndAddSplits(expense, splitType, splitInputs, group.getMembers());

        return expenseRepository.save(expense);
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

            BigDecimal splitAmount = totalAmount.divide(new BigDecimal(involved.size()), 0, RoundingMode.CEILING);
            for (User member : involved) {
                expense.addSplit(new ExpenseSplit(member, splitAmount));
            }
        } else if (splitType == SplitType.EXACT) {
            for (User member : allMembers) {
                BigDecimal share = splitInputs.getOrDefault(member.getId(), BigDecimal.ZERO).setScale(0,
                        RoundingMode.CEILING);
                if (share.compareTo(BigDecimal.ZERO) > 0) {
                    expense.addSplit(new ExpenseSplit(member, share));
                }
            }
        } else if (splitType == SplitType.PERCENTAGE) {
            for (User member : allMembers) {
                BigDecimal percent = splitInputs.getOrDefault(member.getId(), BigDecimal.ZERO);
                if (percent.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal share = totalAmount.multiply(percent).divide(new BigDecimal("100"), 0,
                            RoundingMode.CEILING);
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
                    BigDecimal share = shareValue.multiply(memberShares).setScale(0, RoundingMode.CEILING);
                    expense.addSplit(new ExpenseSplit(member, share));
                }
            }
        }

        // Automatically mark the payer's split as settled/paid
        for (ExpenseSplit split : expense.getSplits()) {
            if (split.getUser().getId().equals(expense.getPaidBy().getId())) {
                split.setPaid(true);
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
}
