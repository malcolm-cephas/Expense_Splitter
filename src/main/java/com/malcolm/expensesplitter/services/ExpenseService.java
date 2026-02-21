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
}
