package com.malcolm.expensesplitter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.malcolm.expensesplitter.dto.GroupBackupDto;
import com.malcolm.expensesplitter.models.*;
import com.malcolm.expensesplitter.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class ImportService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private final ObjectMapper objectMapper;

    public ImportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Group importGroupFromBackup(File file) throws Exception {
        GroupBackupDto dto = objectMapper.readValue(file, GroupBackupDto.class);

        // 1. Restore Users
        Map<String, User> emailToUserMap = new HashMap<>();
        for (GroupBackupDto.UserBackupDto ud : dto.members) {
            User user = userRepository.findByEmail(ud.email).orElseGet(() -> {
                User newUser = new User(ud.name, ud.email, ud.currencyPreference);
                newUser.setFamilyName(ud.familyName);
                return userRepository.save(newUser);
            });
            // Update family name if it's missing in DB but present in backup
            if ((user.getFamilyName() == null || user.getFamilyName().isEmpty()) && ud.familyName != null) {
                user.setFamilyName(ud.familyName);
                userRepository.save(user);
            }
            emailToUserMap.put(ud.email, user);
        }

        // 2. Create Group
        Group group = new Group();
        group.setName(dto.name + " (Imported)");
        group.setDescription(dto.description);
        group.setBudget(dto.budget);
        group.setBudgetCurrency(dto.budgetCurrency);
        group.setFamilyGroupingEnabled(dto.familyGroupingEnabled);
        
        for (User user : emailToUserMap.values()) {
            group.getMembers().add(user);
        }
        
        // Use first member as creator if no mapping (or add logic if needed)
        if (!dto.members.isEmpty()) {
            group.setCreatedBy(emailToUserMap.get(dto.members.get(0).email));
        }

        group = groupRepository.save(group);

        // 3. Restore Expenses
        for (GroupBackupDto.ExpenseBackupDto ed : dto.expenses) {
            Expense expense = new Expense();
            expense.setGroup(group);
            expense.setDescription(ed.description);
            expense.setAmount(ed.amount);
            expense.setCurrency(ed.currency);
            expense.setCategory(ed.category);
            expense.setPaymentMode(ed.paymentMode);
            expense.setSplitType(ed.splitType != null ? SplitType.valueOf(ed.splitType) : SplitType.EQUAL);
            expense.setCreatedAt(ed.createdAt != null ? ed.createdAt : java.time.LocalDateTime.now());
            expense.setExpenseDate(ed.expenseDate != null ? ed.expenseDate : java.time.LocalDate.now());

            // Restore Payments
            for (GroupBackupDto.PaymentBackupDto pd : ed.payments) {
                User payer = emailToUserMap.get(pd.userEmail);
                if (payer != null) {
                    ExpensePayment payment = new ExpensePayment(payer, pd.amount);
                    expense.addPayment(payment);
                    // Set legacy paidBy for DB constraint compatibility
                    if (expense.getLegacyPaidBy() == null) {
                        expense.setLegacyPaidBy(payer);
                    }
                }
            }

            // Restore Splits
            for (GroupBackupDto.SplitBackupDto sd : ed.splits) {
                User splitter = emailToUserMap.get(sd.userEmail);
                if (splitter != null) {
                    ExpenseSplit split = new ExpenseSplit(splitter, sd.owedAmount);
                    split.setPaidAmount(sd.paidAmount);
                    split.setPaid(sd.isPaid);
                    expense.addSplit(split);
                }
            }

            expenseRepository.save(expense);
        }

        return group;
    }
}
