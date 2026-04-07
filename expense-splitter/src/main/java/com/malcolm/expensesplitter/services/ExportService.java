package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import com.malcolm.expensesplitter.config.AppConfig;

import com.malcolm.expensesplitter.dto.GroupBackupDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;

/**
 * Service responsible for exporting group financial data to external formats.
 * Currently supports PDF (using iText) and CSV.
 */
@Service
public class ExportService {

        @Autowired
        private AppConfig appConfig;

        @Autowired
        private ExchangeRateService exchangeRateService;

        private final ObjectMapper objectMapper;

        public ExportService() {
                this.objectMapper = new ObjectMapper();
                this.objectMapper.registerModule(new JavaTimeModule());
        }

        /**
         * Exports group summary, user balances, and suggested settlements to a PDF
         * file.
         * 
         * @param group       The group to export.
         * @param expenses    The list of expenses to include.
         * @param settlements The list of suggested simplified settlements.
         * @param file        The target PDF file.
         * @throws Exception If PDF generation fails.
         */
        public void exportGroupToPdf(Group group, List<Expense> expenses, List<TransactionDto> settlements, File file)
                        throws Exception {
                PdfWriter writer = new PdfWriter(file);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                List<Expense> sortedExpenses = new java.util.ArrayList<>(expenses);
                sortedExpenses.sort(java.util.Comparator.comparing(Expense::getExpenseDate)
                                .thenComparing(Expense::getCreatedAt));

                // Header Section
                document.add(new Paragraph("EXPENSE SPLITTER REPORT")
                                .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("Group: " + group.getName()).setBold().setFontSize(14));
                document.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().toString().substring(0, 19)));

                // Budget Info in PDF Header
                BigDecimal budget = group.getBudget();
                if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
                        String budgetCurrency = group.getBudgetCurrency() != null ? group.getBudgetCurrency()
                                        : appConfig.getCurrencyCode();
                        String budgetSymbol = appConfig.getSymbol(budgetCurrency);

                        // Calculate total spent in budget currency (simplified for now by using
                        // prefCode if needed,
                        // but for accuracy we'll just sum all expenses converted to budget currency)
                        BigDecimal totalSpent = BigDecimal.ZERO;
                        for (Expense e : expenses) {
                                BigDecimal rate = exchangeRateService.getExchangeRate(e.getCurrency(), budgetCurrency);
                                totalSpent = totalSpent.add(e.getAmount().multiply(rate));
                        }
                        BigDecimal remaining = budget.subtract(totalSpent);

                        Paragraph budgetPara = new Paragraph()
                                        .add(new Text("Budget: ").setBold())
                                        .add(new Text(budgetSymbol + appConfig.formatAmount(budget, budgetCurrency)
                                                        + " " + budgetCurrency))
                                        .add(new Text("  |  "))
                                        .add(new Text("Total Spent: ").setBold())
                                        .add(new Text(budgetSymbol + appConfig.formatAmount(totalSpent, budgetCurrency)))
                                        .add(new Text("  |  "))
                                        .add(new Text("Remaining: ").setBold());

                        Text remainingText = new Text(budgetSymbol + appConfig.formatAmount(remaining, budgetCurrency));
                        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                                remainingText.setFontColor(ColorConstants.RED);
                        } else {
                                remainingText.setFontColor(ColorConstants.GREEN);
                        }
                        budgetPara.add(remainingText);
                        document.add(budgetPara);
                }
                document.add(new Paragraph("\n"));

                // 1. Expenses Summary Table
                // This table gives a high-level overview of each transaction in the group.
                document.add(new Paragraph("1. EXPENSE SUMMARY").setBold());
                float[] summaryWidths = { 2, 2.5f, 1.5f, 2, 2, 1, 6 };
                Table summaryTable = new Table(UnitValue.createPercentArray(summaryWidths)).useAllAvailableWidth();
                summaryTable.addHeaderCell(
                                new Cell().add(new Paragraph("Date")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                summaryTable.addHeaderCell(
                                new Cell().add(new Paragraph("Description"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                summaryTable
                                .addHeaderCell(new Cell().add(new Paragraph("Category"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                summaryTable
                                .addHeaderCell(new Cell().add(new Paragraph("Amount"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                summaryTable
                                .addHeaderCell(new Cell().add(new Paragraph("Paid By"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                summaryTable
                                .addHeaderCell(new Cell().add(new Paragraph("Status"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                summaryTable.addHeaderCell(
                                new Cell().add(new Paragraph("Involved (Share)"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                for (Expense e : sortedExpenses) {
                        boolean allSettled = e.getSplits().stream().allMatch(s -> s.isPaid());
                        String statusText = allSettled ? "Settled" : "Pending";

                        Paragraph involvedPara = new Paragraph().setFontSize(9);
                        if (group.isFamilyGroupingEnabled()) {
                                // Grouping by family for the "Involved" column
                                java.util.Map<String, List<com.malcolm.expensesplitter.models.ExpenseSplit>> familySplits = new java.util.LinkedHashMap<>();
                                for (com.malcolm.expensesplitter.models.ExpenseSplit s : e.getSplits()) {
                                        String family = s.getUser().getFamilyName();
                                        if (family == null || family.trim().isEmpty())
                                                family = s.getUser().getName();
                                        familySplits.computeIfAbsent(family, k -> new java.util.ArrayList<>()).add(s);
                                }

                                int famIdx = 0;
                                for (java.util.Map.Entry<String, List<com.malcolm.expensesplitter.models.ExpenseSplit>> entry : familySplits
                                                .entrySet()) {
                                        String famName = entry.getKey();
                                        List<com.malcolm.expensesplitter.models.ExpenseSplit> splits = entry.getValue();

                                        BigDecimal familyOwed = splits.stream()
                                                        .map(com.malcolm.expensesplitter.models.ExpenseSplit::getOwedAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        boolean familyAllSettled = splits.stream().allMatch(
                                                        com.malcolm.expensesplitter.models.ExpenseSplit::isPaid);

                                        involvedPara.add(new Text("[" + splits.size() + " mem] ").setBold());
                                        involvedPara.add(new Text(famName).setBold());
                                        involvedPara.add(new Text(": " + e.getCurrency() + " "
                                                        + appConfig.formatAmount(familyOwed, e.getCurrency())));

                                        if (familyAllSettled) {
                                                involvedPara.add(new Text(" [Settled]")
                                                                .setFontColor(ColorConstants.GREEN).setBold());
                                        }

                                        // Show individual members if it's a family or multiple members are under this
                                        // name
                                        if (splits.size() > 1 || !splits.get(0).getUser().getName().equals(famName)) {
                                                String members = splits.stream()
                                                                .map(split -> split.getUser().getName())
                                                                .collect(Collectors.joining(", "));
                                                involvedPara.add(new Text("\n(" + members + ")").setFontSize(7)
                                                                .setItalic());
                                        }

                                        if (famIdx < familySplits.size() - 1) {
                                                involvedPara.add(new Text("\n---\n"));
                                        }
                                        famIdx++;
                                }
                        } else {
                                // Standard individual display
                                for (int i = 0; i < e.getSplits().size(); i++) {
                                        com.malcolm.expensesplitter.models.ExpenseSplit s = e.getSplits().get(i);
                                        involvedPara.add(new Text(s.getUser().getName()));
                                        if (s.isPaid()) {
                                                involvedPara.add(new Text(" [Settled]")
                                                                .setFontColor(ColorConstants.GREEN));
                                        }
                                        involvedPara.add(new Text(" ("
                                                        + e.getCurrency() + " "
                                                        + appConfig.formatAmount(s.getOwedAmount(), e.getCurrency())
                                                        + ")"));
                                        if (i < e.getSplits().size() - 1) {
                                                involvedPara.add(new Text("\n"));
                                        }
                                }
                        }

                        String paidByStr = e.getPayments().stream()
                                        .map(p -> p.getUser().getName() + " ("
                                                        + e.getCurrency() + " "
                                                        + appConfig.formatAmount(p.getAmount(), e.getCurrency())
                                                        + ")")
                                        .collect(Collectors.joining("\n"));

                        summaryTable.addCell(e.getExpenseDate().toString());
                        summaryTable.addCell(e.getDescription());
                        summaryTable.addCell(e.getCategory() != null ? e.getCategory() : "Other");
                        summaryTable.addCell(appConfig.formatAmount(e.getAmount(), e.getCurrency()) + " "
                                        + e.getCurrency());
                        summaryTable.addCell(new Cell().add(new Paragraph(paidByStr).setFontSize(9)));

                        Cell statusCell = new Cell().add(new Paragraph(statusText));
                        if (allSettled) {
                                statusCell.setFontColor(ColorConstants.GREEN);
                        }
                        summaryTable.addCell(statusCell);

                        summaryTable.addCell(new Cell().add(involvedPara));
                }
                document.add(summaryTable);
                document.add(new Paragraph("\n"));

                // 2. User Balances Table
                // Shows how much each user has paid versus their calculated total debt share.
                document.add(new Paragraph("2. FINAL SETTLE UP (BALANCES)").setBold());
                float[] balanceWidths = { 4, 3, 3, 3 };
                Table balanceTable = new Table(UnitValue.createPercentArray(balanceWidths)).useAllAvailableWidth();
                balanceTable.addHeaderCell(
                                new Cell().add(new Paragraph("User")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                balanceTable.addHeaderCell(
                                new Cell().add(new Paragraph("Total Paid"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                balanceTable.addHeaderCell(
                                new Cell().add(new Paragraph("Total Share"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                balanceTable.addHeaderCell(
                                new Cell().add(new Paragraph("Net Balance"))
                                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                Map<UUID, BigDecimal> totalPaidBy = new HashMap<>();
                Map<UUID, BigDecimal> totalShareOf = new HashMap<>();
                for (User member : group.getMembers()) {
                        totalPaidBy.put(member.getId(), BigDecimal.ZERO);
                        totalShareOf.put(member.getId(), BigDecimal.ZERO);
                }
                String prefCode = appConfig.getCurrencyCode();
                for (Expense e : sortedExpenses) {
                        BigDecimal rate = BigDecimal.ONE;
                        if (e.getCurrency() != null && !e.getCurrency().equalsIgnoreCase(prefCode)) {
                                rate = exchangeRateService.getExchangeRate(e.getCurrency(), prefCode);
                        }

                        for (com.malcolm.expensesplitter.models.ExpensePayment payment : e.getPayments()) {
                                UUID payerId = payment.getUser().getId();
                                BigDecimal conv = payment.getAmount().multiply(rate);
                                totalPaidBy.put(payerId, totalPaidBy.get(payerId).add(conv));
                        }
                        for (com.malcolm.expensesplitter.models.ExpenseSplit split : e.getSplits()) {
                                UUID userId = split.getUser().getId();
                                BigDecimal conv = split.getOwedAmount().multiply(rate);
                                totalShareOf.put(userId, totalShareOf.get(userId).add(conv));
                        }
                }

                String prefSymbol = appConfig.getSymbol(prefCode);
                if (group.isFamilyGroupingEnabled()) {
                        // Use TreeMap to keep family names sorted alphabetically
                        Map<String, BigDecimal> familyPaid = new java.util.TreeMap<>();
                        Map<String, BigDecimal> familyShare = new java.util.TreeMap<>();
                        Map<String, List<String>> familyMemberNames = new java.util.TreeMap<>();

                        for (User member : group.getMembers()) {
                                String key = member.getFamilyName();
                                if (key == null || key.trim().isEmpty())
                                        key = member.getName();

                                familyPaid.put(key, familyPaid.getOrDefault(key, BigDecimal.ZERO)
                                                .add(totalPaidBy.get(member.getId())));
                                familyShare.put(key, familyShare.getOrDefault(key, BigDecimal.ZERO)
                                                .add(totalShareOf.get(member.getId())));
                                familyMemberNames.computeIfAbsent(key, k -> new ArrayList<>()).add(member.getName());
                        }

                        for (String fam : familyMemberNames.keySet()) {
                                BigDecimal paid = familyPaid.get(fam);
                                BigDecimal share = familyShare.get(fam);
                                BigDecimal balance = paid.subtract(share);
                                String membersStr = fam;
                                if (familyMemberNames.get(fam).size() > 1
                                                || !familyMemberNames.get(fam).get(0).equals(fam)) {
                                        membersStr = fam + " (" + String.join(", ", familyMemberNames.get(fam)) + ")";
                                }
                                balanceTable.addCell(new Cell().add(new Paragraph(membersStr).setFontSize(9)));
                                balanceTable.addCell(prefSymbol + " " + appConfig.formatAmount(paid));
                                balanceTable.addCell(prefSymbol + " " + appConfig.formatAmount(share));
                                balanceTable.addCell(prefSymbol + " " + appConfig.formatAmount(balance));
                        }
                } else {
                        List<User> sortedMembers = new java.util.ArrayList<>(group.getMembers());
                        sortedMembers.sort(java.util.Comparator.comparing(User::getName));
                        for (User member : sortedMembers) {
                                BigDecimal paid = totalPaidBy.get(member.getId());
                                BigDecimal share = totalShareOf.get(member.getId());
                                BigDecimal balance = paid.subtract(share);
                                balanceTable.addCell(member.getName());
                                balanceTable.addCell(prefSymbol + " " + appConfig.formatAmount(paid));
                                balanceTable.addCell(prefSymbol + " " + appConfig.formatAmount(share));
                                balanceTable.addCell(prefSymbol + " " + appConfig.formatAmount(balance));
                        }
                }
                document.add(balanceTable);

                // 3. Suggested Settlements
                // These are the simplified transactions required to clear all debts within the
                // group.
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("3. SUGGESTED SETTLEMENTS").setBold());
                if (settlements.isEmpty()) {
                        document.add(new Paragraph("All settled up!"));
                } else {
                        Map<UUID, String> userNames = group.getMembers().stream()
                                        .collect(Collectors.toMap(User::getId, User::getName));

                        // Sort settlements by 'From' name
                        List<TransactionDto> sortedSettlements = new java.util.ArrayList<>(settlements);
                        sortedSettlements.sort(java.util.Comparator.comparing(t -> {
                                String name = t.getFromName();
                                if (name == null && t.getFrom() != null)
                                        name = userNames.get(t.getFrom());
                                return name != null ? name : "Unknown";
                        }));

                        Table settTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1, 1 }))
                                        .useAllAvailableWidth();
                        settTable.addHeaderCell("From");
                        settTable.addHeaderCell("Action");
                        settTable.addHeaderCell("To");
                        settTable.addHeaderCell("Amount");

                        for (TransactionDto t : sortedSettlements) {
                                String from = t.getFromName() != null ? t.getFromName() : userNames.get(t.getFrom());
                                String to = t.getToName() != null ? t.getToName() : userNames.get(t.getTo());
                                settTable.addCell(from);
                                settTable.addCell("owes");
                                settTable.addCell(to);
                                String currencyCode = appConfig.getCurrencyCode();
                                String symbol = appConfig.getSymbol(currencyCode);
                                settTable.addCell(symbol + " "
                                                + appConfig.formatAmount(t.getAmount())
                                                + " " + currencyCode);
                        }
                        document.add(settTable);
                }

                // 4. Detailed Splits (Placed on a new page for clarity)
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                document.add(new Paragraph("4. DETAILED INDIVIDUAL SPLITS").setBold().setFontSize(16));
                float[] splitWidths = { 3, 4, 2, 2, 2 };
                Table splitsTable = new Table(UnitValue.createPercentArray(splitWidths)).useAllAvailableWidth();
                splitsTable.addHeaderCell(
                                new Cell().add(new Paragraph("Member")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                splitsTable.addHeaderCell(
                                new Cell().add(new Paragraph("Expense")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                splitsTable.addHeaderCell(
                                new Cell().add(new Paragraph("Owed")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                splitsTable.addHeaderCell(
                                new Cell().add(new Paragraph("Paid")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                splitsTable.addHeaderCell(
                                new Cell().add(new Paragraph("Status")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

                List<User> sortedMembersPdf = new java.util.ArrayList<>(group.getMembers());
                sortedMembersPdf.sort(java.util.Comparator.comparing(User::getName));
                for (User member : sortedMembersPdf) {
                        for (Expense e : sortedExpenses) {
                                for (com.malcolm.expensesplitter.models.ExpenseSplit split : e.getSplits()) {
                                        if (split.getUser().getId().equals(member.getId())) {
                                                splitsTable.addCell(member.getName());
                                                splitsTable.addCell(e.getDescription());
                                                splitsTable.addCell(e.getCurrency() + " " + appConfig.formatAmount(split.getOwedAmount(), e.getCurrency()));
                                                splitsTable.addCell(e.getCurrency() + " " + appConfig.formatAmount(split.getPaidAmount(), e.getCurrency()));

                                                Cell statusCell = new Cell().add(
                                                                new Paragraph(split.isPaid() ? "Settled" : "Pending"));
                                                if (split.isPaid()) {
                                                        statusCell.setFontColor(ColorConstants.GREEN);
                                                }
                                                splitsTable.addCell(statusCell);
                                        }
                                }
                        }
                }
                document.add(splitsTable);

                document.close();
        }

        /**
         * Exports group financial data to a CSV file.
         * 
         * @param group       The group to export.
         * @param expenses    The list of expenses.
         * @param settlements Simplified suggestions.
         * @param writer      PrintWriter for the file.
         */
        public void exportGroupToCsv(Group group, List<Expense> expenses, List<TransactionDto> settlements,
                        PrintWriter writer) {
                List<Expense> sortedExpenses = new java.util.ArrayList<>(expenses);
                sortedExpenses.sort(java.util.Comparator.comparing(Expense::getExpenseDate)
                                .thenComparing(Expense::getCreatedAt));
                // Headers
                writer.println("EXPENSE SPLITTER REPORT");
                writer.println("Group Name," + escapeCsv(group.getName()));
                writer.println("Total Members," + group.getMembers().size());
                writer.println("Generated At," + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")));

                BigDecimal budget = group.getBudget();
                if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
                        String budgetCurrency = group.getBudgetCurrency() != null ? group.getBudgetCurrency()
                                        : appConfig.getCurrencyCode();
                        BigDecimal totalSpent = BigDecimal.ZERO;
                        for (Expense e : expenses) {
                                BigDecimal rate = exchangeRateService.getExchangeRate(e.getCurrency(), budgetCurrency);
                                totalSpent = totalSpent.add(e.getAmount().multiply(rate));
                        }
                        BigDecimal remaining = budget.subtract(totalSpent);
                        writer.println("Group Budget," + budget.setScale(2, java.math.RoundingMode.HALF_UP) + " "
                                        + budgetCurrency);
                        writer.println("Total Spent," + totalSpent.setScale(2, java.math.RoundingMode.HALF_UP) + " "
                                        + budgetCurrency);
                        writer.println("Remaining Balance," + remaining.setScale(2, java.math.RoundingMode.HALF_UP)
                                        + " " + budgetCurrency);
                }
                writer.println();

                // 1. Expenses Summary Table
                writer.println("--- EXPENSE SUMMARY ---");
                writer.println("Date,Description,Category,Total Amount,Paid By,Payment Mode,Status,Involved Members (Shares)");
                for (Expense e : sortedExpenses) {
                        String status = e.getSplits().stream()
                                        .allMatch(com.malcolm.expensesplitter.models.ExpenseSplit::isPaid) ? "Settled"
                                                        : "Pending";
                        String involved;
                        if (group.isFamilyGroupingEnabled()) {
                                java.util.Map<String, List<com.malcolm.expensesplitter.models.ExpenseSplit>> familySplits = new java.util.LinkedHashMap<>();
                                for (com.malcolm.expensesplitter.models.ExpenseSplit s : e.getSplits()) {
                                        String family = s.getUser().getFamilyName();
                                        if (family == null || family.trim().isEmpty())
                                                family = s.getUser().getName();
                                        familySplits.computeIfAbsent(family, k -> new java.util.ArrayList<>()).add(s);
                                }

                                involved = familySplits.entrySet().stream()
                                                .map(entry -> {
                                                        String famName = entry.getKey();
                                                        List<com.malcolm.expensesplitter.models.ExpenseSplit> splits = entry
                                                                        .getValue();
                                                        BigDecimal famAmount = splits.stream().map(
                                                                        com.malcolm.expensesplitter.models.ExpenseSplit::getOwedAmount)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                                        boolean allPaid = splits.stream().allMatch(
                                                                        com.malcolm.expensesplitter.models.ExpenseSplit::isPaid);

                                                        String membersInfo = "";
                                                        if (splits.size() > 1 || !splits.get(0).getUser().getName()
                                                                        .equals(famName)) {
                                                                membersInfo = " (" + splits.stream()
                                                                                .map(s -> s.getUser().getName())
                                                                                .collect(Collectors.joining(", "))
                                                                                + ")";
                                                        }

                                                        String memInfo = " [" + splits.size() + " mem]";
                                                        return famName + membersInfo + memInfo
                                                                        + (allPaid ? " [Settled]" : "") + ": "
                                                                        + famAmount.setScale(2,
                                                                                        java.math.RoundingMode.HALF_UP);
                                                })
                                                .collect(Collectors.joining(" | "));
                        } else {
                                involved = e.getSplits().stream()
                                                .map(s -> s.getUser().getName() + (s.isPaid() ? " [Settled]" : "")
                                                                + ": "
                                                                + s.getOwedAmount().setScale(2,
                                                                                java.math.RoundingMode.HALF_UP))
                                                .collect(Collectors.joining(" | "));
                        }

                        String paidByStr = e.getPayments().stream()
                                        .map(p -> p.getUser().getName() + " ("
                                                        + e.getCurrency() + " "
                                                        + p.getAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                                                        + ")")
                                        .collect(Collectors.joining(" | "));

                        writer.println(e.getExpenseDate().toString() + "," +
                                        escapeCsv(e.getDescription()) + "," +
                                        escapeCsv(e.getCategory() != null ? e.getCategory() : "Other") + "," +
                                        e.getAmount().setScale(2, java.math.RoundingMode.HALF_UP) + " "
                                        + e.getCurrency() + "," +
                                        escapeCsv(paidByStr) + "," +
                                        escapeCsv(e.getPaymentMode() != null ? e.getPaymentMode() : "Cash") + "," +
                                        status + "," +
                                        escapeCsv(involved));
                }
                writer.println();

                // 2. Detailed Splits Table
                writer.println("--- DETAILED INDIVIDUAL SPLITS ---");
                writer.println("Member Name,Expense Description,Owed Amount,Paid Amount,Status");
                List<User> sortedMembersCsv = new java.util.ArrayList<>(group.getMembers());
                sortedMembersCsv.sort(java.util.Comparator.comparing(User::getName));
                for (User member : sortedMembersCsv) {
                        for (Expense e : sortedExpenses) {
                                for (com.malcolm.expensesplitter.models.ExpenseSplit split : e.getSplits()) {
                                        if (split.getUser().getId().equals(member.getId())) {
                                                String splitStatus = split.isPaid() ? "Settled" : "Pending";
                                                writer.println(escapeCsv(member.getName()) + "," +
                                                                escapeCsv(e.getDescription()) + "," +
                                                                e.getCurrency() + " "
                                                                + split.getOwedAmount().setScale(2,
                                                                                java.math.RoundingMode.HALF_UP)
                                                                + ","
                                                                +
                                                                e.getCurrency() + " "
                                                                + split.getPaidAmount().setScale(2,
                                                                                java.math.RoundingMode.HALF_UP)
                                                                + ","
                                                                +
                                                                splitStatus);
                                        }
                                }
                        }
                }
                writer.println();

                // 3. User Balances Table (Settle Up Table)
                writer.println("--- FINAL SETTLE UP TABLE (BALANCES) ---");
                writer.println("User,Total Paid (As Payer),Total Share (Own Expenses),Net Balance (Positive = Is Owed)");
                Map<UUID, String> userNames = group.getMembers().stream()
                                .collect(Collectors.toMap(User::getId, User::getName));
                Map<UUID, BigDecimal> totalPaidBy = new HashMap<>(); // How much they paid for everyone
                Map<UUID, BigDecimal> totalShareOf = new HashMap<>(); // How much they owe for their share

                for (User member : group.getMembers()) {
                        totalPaidBy.put(member.getId(), BigDecimal.ZERO);
                        totalShareOf.put(member.getId(), BigDecimal.ZERO);
                }

                String prefCode = appConfig.getCurrencyCode();
                for (Expense e : sortedExpenses) {
                        BigDecimal rate = BigDecimal.ONE;
                        if (e.getCurrency() != null && !e.getCurrency().equalsIgnoreCase(prefCode)) {
                                rate = exchangeRateService.getExchangeRate(e.getCurrency(), prefCode);
                        }

                        for (com.malcolm.expensesplitter.models.ExpensePayment payment : e.getPayments()) {
                                UUID payerId = payment.getUser().getId();
                                BigDecimal conv = payment.getAmount().multiply(rate);
                                totalPaidBy.put(payerId, totalPaidBy.getOrDefault(payerId, BigDecimal.ZERO).add(conv));
                        }
                        for (com.malcolm.expensesplitter.models.ExpenseSplit split : e.getSplits()) {
                                UUID memberId = split.getUser().getId();
                                BigDecimal conv = split.getOwedAmount().multiply(rate);
                                totalShareOf.put(memberId,
                                                totalShareOf.getOrDefault(memberId, BigDecimal.ZERO).add(conv));
                        }
                }

                if (group.isFamilyGroupingEnabled()) {
                        // Use TreeMap to keep family names sorted alphabetically
                        Map<String, BigDecimal> familyPaid = new java.util.TreeMap<>();
                        Map<String, BigDecimal> familyShare = new java.util.TreeMap<>();

                        for (User member : group.getMembers()) {
                                String key = member.getFamilyName();
                                if (key == null || key.trim().isEmpty())
                                        key = member.getName();

                                familyPaid.put(key, familyPaid.getOrDefault(key, BigDecimal.ZERO)
                                                .add(totalPaidBy.get(member.getId())));
                                familyShare.put(key, familyShare.getOrDefault(key, BigDecimal.ZERO)
                                                .add(totalShareOf.get(member.getId())));
                        }

                        for (String fam : familyPaid.keySet()) {
                                BigDecimal paid = familyPaid.get(fam);
                                BigDecimal share = familyShare.get(fam);
                                BigDecimal balance = paid.subtract(share);
                                writer.println(escapeCsv(fam) + "," +
                                                paid.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode + ","
                                                +
                                                share.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode + ","
                                                +
                                                balance.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode);
                        }
                } else {
                        List<User> sortedMembers = new java.util.ArrayList<>(group.getMembers());
                        sortedMembers.sort(java.util.Comparator.comparing(User::getName));
                        for (User member : sortedMembers) {
                                BigDecimal paid = totalPaidBy.get(member.getId());
                                BigDecimal share = totalShareOf.get(member.getId());
                                BigDecimal balance = paid.subtract(share);
                                writer.println(escapeCsv(member.getName()) + "," +
                                                paid.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode + ","
                                                +
                                                share.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode + ","
                                                +
                                                balance.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode);
                        }
                }
                writer.println();

                // 4. Suggested Settlements Section
                writer.println("--- SUGGESTED SETTLEMENTS (TO CLEAR DEBTS) ---");
                writer.println("From,Action,To,Amount");
                if (settlements.isEmpty()) {
                        writer.println("All settled up!,,,");
                } else {
                        // Sort settlements by 'From' name
                        List<TransactionDto> sortedSettlements = new java.util.ArrayList<>(settlements);
                        sortedSettlements.sort(java.util.Comparator.comparing(t -> {
                                String name = t.getFromName();
                                if (name == null && t.getFrom() != null)
                                        name = userNames.get(t.getFrom());
                                return name != null ? name : "Unknown";
                        }));

                        for (TransactionDto t : sortedSettlements) {
                                String from = t.getFromName() != null ? t.getFromName() : userNames.get(t.getFrom());
                                String to = t.getToName() != null ? t.getToName() : userNames.get(t.getTo());
                                writer.println(escapeCsv(from) + ",owes," +
                                                escapeCsv(to) + "," +
                                                appConfig.getCurrencyCode() + " " +
                                                t.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
                        }
                }
        }

        public void exportGroupToJsonBackup(Group group, List<Expense> expenses, File file) throws Exception {
                GroupBackupDto dto = new GroupBackupDto();
                dto.name = group.getName();
                dto.description = group.getDescription();
                dto.budget = group.getBudget();
                dto.budgetCurrency = group.getBudgetCurrency();
                dto.familyGroupingEnabled = group.isFamilyGroupingEnabled();

                dto.members = group.getMembers().stream().map(m -> {
                        GroupBackupDto.UserBackupDto ud = new GroupBackupDto.UserBackupDto();
                        ud.name = m.getName();
                        ud.email = m.getEmail();
                        ud.currencyPreference = m.getCurrencyPreference();
                        ud.familyName = m.getFamilyName();
                        return ud;
                }).collect(Collectors.toList());

                dto.expenses = expenses.stream().map(e -> {
                        GroupBackupDto.ExpenseBackupDto ed = new GroupBackupDto.ExpenseBackupDto();
                        ed.description = e.getDescription();
                        ed.amount = e.getAmount();
                        ed.currency = e.getCurrency();
                        ed.category = e.getCategory();
                        ed.paymentMode = e.getPaymentMode();
                        ed.splitType = e.getSplitType() != null ? e.getSplitType().name() : null;
                        ed.createdAt = e.getCreatedAt();
                        ed.expenseDate = e.getExpenseDate();

                        ed.payments = e.getPayments().stream().map(p -> {
                                GroupBackupDto.PaymentBackupDto pd = new GroupBackupDto.PaymentBackupDto();
                                pd.userEmail = p.getUser().getEmail();
                                pd.amount = p.getAmount();
                                return pd;
                        }).collect(Collectors.toList());

                        ed.splits = e.getSplits().stream().map(s -> {
                                GroupBackupDto.SplitBackupDto sd = new GroupBackupDto.SplitBackupDto();
                                sd.userEmail = s.getUser().getEmail();
                                sd.owedAmount = s.getOwedAmount();
                                sd.paidAmount = s.getPaidAmount();
                                sd.isPaid = s.isPaid();
                                return sd;
                        }).collect(Collectors.toList());

                        return ed;
                }).collect(Collectors.toList());

                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, dto);
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
