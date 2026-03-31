package com.malcolm.expensesplitter.controllers.rest;

import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.SplitType;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import com.malcolm.expensesplitter.services.ExpenseService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/expenses")
public class ExpenseRestController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping
    public ResponseEntity<Page<Expense>> searchExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Specification<Expense> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (minAmount != null) {
                predicates.add(cb.ge(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.le(root.get("amount"), maxAmount));
            }
            if (groupId != null) {
                predicates.add(cb.equal(root.get("group").get("id"), groupId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), toDate));
            }
            if (description != null) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }
            if (userId != null) {
                // Check if user is payer or involved in splits
                // Simplified: check payments
                predicates.add(cb.equal(root.join("payments").get("user").get("id"), userId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(expenseRepository.findAll(spec, pageable));
    }

    @PostMapping
    public ResponseEntity<?> createExpense(
            @RequestParam UUID groupId,
            @RequestParam UUID paidById,
            @RequestParam BigDecimal amount,
            @RequestParam String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
            @RequestParam SplitType splitType,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) boolean ignoreDuplicate) {

        if (!ignoreDuplicate) {
            List<Expense> duplicates = expenseService.findPotentialDuplicates(paidById, amount, description);
            if (!duplicates.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("warning", "Possible duplicate expense detected");
                response.put("allowOverride", true);
                return ResponseEntity.status(200).body(response);
            }
        }

        // Mocking splitInputs as equal for all group members if not provided
        // In a real API, we'd accept Map<UUID, BigDecimal> splitInputs
        Map<UUID, BigDecimal> splitInputs = new HashMap<>(); 
        Expense expense = expenseService.addExpense(groupId, paidById, amount, description, 
                paymentMode != null ? paymentMode : "Other", 
                category, 
                expenseDate != null ? expenseDate : LocalDate.now(), 
                splitType, splitInputs, 
                currency != null ? currency : "INR", null);

        return ResponseEntity.ok(expense);
    }
}
