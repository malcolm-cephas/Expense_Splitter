package com.malcolm.expensesplitter.controllers.rest;

import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.SplitType;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import com.malcolm.expensesplitter.services.ExpenseService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.malcolm.expensesplitter.services.UserService;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.repositories.GroupRepository;
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

    @Autowired
    private UserService userService;

    @Autowired
    private GroupRepository groupRepository;

    @GetMapping
    public ResponseEntity<?> searchExpenses(
            @AuthenticationPrincipal Jwt jwt,
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
        User user = userService.getOrCreateUserFromJwt(jwt);

        // Security: Ensure search is restricted to groups the user belongs to
        Specification<Expense> membershipSpec = (root, query, cb) -> {
             return cb.equal(root.join("group").join("members").get("id"), user.getId());
        };
        
        return ResponseEntity.ok(expenseRepository.findAll(Specification.where(spec).and(membershipSpec), pageable));
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
            @RequestParam(required = false) boolean ignoreDuplicate,
            @AuthenticationPrincipal Jwt jwt) {

        User user = userService.getOrCreateUserFromJwt(jwt);
        Group group = groupRepository.findById(groupId).orElseThrow();
        
        // Security: Check membership
        if (!group.getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }

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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount,
            @RequestParam String description,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        Expense expense = expenseRepository.findById(id).orElseThrow();
        if (!expense.getGroup().getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }
        expense.setAmount(amount);
        expense.setDescription(description);
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        Expense expense = expenseRepository.findById(id).orElseThrow();
        if (!expense.getGroup().getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }
        Group group = expense.getGroup();
        group.removeExpense(expense);
        expenseRepository.delete(expense);
        return ResponseEntity.ok().build();
    }
}
