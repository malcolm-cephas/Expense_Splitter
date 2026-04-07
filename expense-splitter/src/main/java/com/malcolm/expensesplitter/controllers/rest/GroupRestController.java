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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.malcolm.expensesplitter.services.UserService;
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

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<Group>> getMyGroups(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        return ResponseEntity.ok(groupRepository.findByMembersId(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Group groupData, @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        groupData.setCreatedBy(user);
        groupData.getMembers().add(user);
        if (groupData.getBudgetCurrency() == null) {
            groupData.setBudgetCurrency("INR");
        }
        return ResponseEntity.ok(groupRepository.save(groupData));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroup(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        Group group = groupRepository.findById(id).orElseThrow();
        
        if (!group.getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{id}/budget")
    public ResponseEntity<BudgetResponse> getGroupBudget(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        Group group = groupRepository.findById(id).orElseThrow();
        
        if (!group.getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }

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
            @RequestParam String currency,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        Group group = groupRepository.findById(id).orElseThrow();

        if (!group.getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }

        group.setBudget(budget);
        group.setBudgetCurrency(currency);
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @GetMapping("/{groupId}/debt-graph")
    public ResponseEntity<DebtGraphResponse> getDebtGraph(@PathVariable UUID groupId, @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        Group group = groupRepository.findById(groupId).orElseThrow();

        if (!group.getMembers().contains(user)) {
             return ResponseEntity.status(403).build();
        }

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

    @Autowired
    private com.malcolm.expensesplitter.repositories.UserRepository userRepository;

    @PostMapping("/{id}/members")
    public ResponseEntity<Group> addMember(
            @PathVariable UUID id,
            @RequestParam String email,
            @AuthenticationPrincipal Jwt jwt) {
        User requester = userService.getOrCreateUserFromJwt(jwt);
        Group group = groupRepository.findById(id).orElseThrow();
        
        // Members or creator can invite (collaborative)
        if (!group.getMembers().contains(requester) && !group.getCreatedBy().equals(requester)) {
             return ResponseEntity.status(403).build();
        }

        // Check if user exists, if not, create a "shadow" user
        User toAdd = userRepository.findByEmail(email).orElseGet(() -> {
             User newUser = new User(email.split("@")[0], email, "INR");
             return userRepository.save(newUser);
        });

        group.getMembers().add(toAdd);
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Group> removeMember(
             @PathVariable UUID id,
             @PathVariable UUID memberId,
             @AuthenticationPrincipal Jwt jwt) {
         User requester = userService.getOrCreateUserFromJwt(jwt);
         Group group = groupRepository.findById(id).orElseThrow();
         
         if (!group.getCreatedBy().equals(requester)) {
              return ResponseEntity.status(403).build(); // Only creator can remove in this basic demo
         }
         
         group.getMembers().removeIf(m -> m.getId().equals(memberId));
         return ResponseEntity.ok(groupRepository.save(group));
    }
}
