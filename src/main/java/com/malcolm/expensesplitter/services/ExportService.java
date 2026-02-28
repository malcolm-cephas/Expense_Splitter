package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExportService {

    public void exportGroupToCsv(Group group, List<Expense> expenses, List<TransactionDto> settlements,
            PrintWriter writer) {
        // Headers
        writer.println("EXPENSE SPLITTER REPORT");
        writer.println("Group Name," + escapeCsv(group.getName()));
        writer.println("Total Members," + group.getMembers().size());
        writer.println();

        // Expenses Table
        writer.println("--- RECENT EXPENSES ---");
        writer.println("Date,Description,Category,Amount,Paid By,Status");
        for (Expense e : expenses) {
            String status = e.getSplits().stream().allMatch(s -> s.isPaid()) ? "Settled" : "Pending";
            writer.println(e.getCreatedAt().toString().substring(0, 10) + "," +
                    escapeCsv(e.getDescription()) + "," +
                    escapeCsv(e.getCategory() != null ? e.getCategory() : "Other") + "," +
                    e.getAmount().setScale(0, java.math.RoundingMode.CEILING) + "," +
                    escapeCsv(e.getPaidBy().getName()) + "," +
                    status);
        }
        writer.println();

        // Settlements Section
        writer.println("--- SUGGESTED SETTLEMENTS ---");
        writer.println("From,Action,To,Amount");
        Map<UUID, String> userNames = group.getMembers().stream().collect(Collectors.toMap(User::getId, User::getName));
        if (settlements.isEmpty()) {
            writer.println("All settled up!,,,");
        } else {
            for (TransactionDto t : settlements) {
                writer.println(escapeCsv(userNames.get(t.getFrom())) + ",owes," +
                        escapeCsv(userNames.get(t.getTo())) + "," +
                        t.getAmount().setScale(0, java.math.RoundingMode.CEILING));
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
