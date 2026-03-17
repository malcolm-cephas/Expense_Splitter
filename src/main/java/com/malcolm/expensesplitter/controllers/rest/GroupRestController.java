package com.malcolm.expensesplitter.controllers.rest;

import com.malcolm.expensesplitter.dto.BudgetResponse;
import com.malcolm.expensesplitter.dto.DebtGraphResponse;
import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.GroupRepository;
import com.malcolm.expensesplitter.services.ExpenseService;
import com.malcolm.expensesplitter.services.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/groups")
public class GroupRestController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private SettlementService settlementService;

    @GetMapping("/{id}/budget")
    public ResponseEntity<BudgetResponse> getGroupBudget(@PathVariable UUID id) {
        Group group = groupRepository.findById(id).orElseThrow();
        List<Expense> expenses = expenseService.getGroupExpenses(id);

        BigDecimal spent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BudgetResponse response = new BudgetResponse(
                group.getBudget() != null ? group.getBudget() : BigDecimal.ZERO,
                spent);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/budget")
    public ResponseEntity<Group> updateGroupBudget(
            @PathVariable UUID id,
            @RequestParam BigDecimal budget,
            @RequestParam String currency) {
        Group group = groupRepository.findById(id).orElseThrow();
        group.setBudget(budget);
        group.setBudgetCurrency(currency);
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @GetMapping("/{groupId}/debt-graph")
    public ResponseEntity<DebtGraphResponse> getDebtGraph(@PathVariable UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        List<TransactionDto> transactions = settlementService.calculateSimplifiedDebts(groupId);

        List<DebtGraphResponse.Node> nodes = group.getMembers().stream()
                .map(User::getName)
                .map(DebtGraphResponse.Node::new)
                .collect(Collectors.toList());

        List<DebtGraphResponse.Edge> edges = transactions.stream()
                .map(tx -> {
                    // We need to find the user name by ID
                    String fromName = group.getMembers().stream()
                            .filter(m -> m.getId().equals(tx.getFrom()))
                            .findFirst()
                            .map(User::getName)
                            .orElse("Unknown");
                    String toName = group.getMembers().stream()
                            .filter(m -> m.getId().equals(tx.getTo()))
                            .findFirst()
                            .map(User::getName)
                            .orElse("Unknown");
                    return new DebtGraphResponse.Edge(fromName, toName, tx.getAmount());
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new DebtGraphResponse(nodes, edges));
    }
}
