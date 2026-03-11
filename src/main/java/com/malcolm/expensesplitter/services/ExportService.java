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
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import com.malcolm.expensesplitter.config.AppConfig;

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

                // Header Section
                document.add(new Paragraph("EXPENSE SPLITTER REPORT")
                                .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("Group: " + group.getName()).setBold().setFontSize(14));
                document.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().toString().substring(0, 19)));
                document.add(new Paragraph("\n"));

                // 1. Expenses Summary Table
                // This table gives a high-level overview of each transaction in the group.
                document.add(new Paragraph("1. EXPENSE SUMMARY").setBold());
                float[] summaryWidths = { 2, 3, 2, 2, 2, 2, 4 };
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

                for (Expense e : expenses) {
                        boolean allSettled = e.getSplits().stream().allMatch(s -> s.isPaid());
                        String statusText = allSettled ? "Settled" : "Pending";

                        Paragraph involvedPara = new Paragraph().setFontSize(9);
                        for (int i = 0; i < e.getSplits().size(); i++) {
                                com.malcolm.expensesplitter.models.ExpenseSplit s = e.getSplits().get(i);
                                involvedPara.add(new Text(s.getUser().getName()));
                                if (s.isPaid()) {
                                        involvedPara.add(new Text(" [Settled]").setFontColor(ColorConstants.GREEN));
                                }
                                involvedPara.add(new Text(" ("
                                                + e.getCurrency() + " "
                                                + s.getOwedAmount().setScale(2, java.math.RoundingMode.HALF_UP) + ")"));
                                if (i < e.getSplits().size() - 1) {
                                        involvedPara.add(new Text("\n"));
                                }
                        }

                        String paidByStr = e.getPayments().stream()
                                        .map(p -> p.getUser().getName() + " ("
                                                        + e.getCurrency() + " "
                                                        + p.getAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                                                        + ")")
                                        .collect(Collectors.joining("\n"));

                        summaryTable.addCell(e.getCreatedAt().toString().substring(0, 10));
                        summaryTable.addCell(e.getDescription());
                        summaryTable.addCell(e.getCategory() != null ? e.getCategory() : "Other");
                        summaryTable.addCell(e.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toString() + " " + e.getCurrency());
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
                for (Expense e : expenses) {
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
                for (User member : group.getMembers()) {
                        BigDecimal paid = totalPaidBy.get(member.getId());
                        BigDecimal share = totalShareOf.get(member.getId());
                        BigDecimal balance = paid.subtract(share);
                        balanceTable.addCell(member.getName());
                        balanceTable.addCell(prefSymbol + " " + paid.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                        balanceTable.addCell(prefSymbol + " " + share.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                        balanceTable.addCell(prefSymbol + " " + balance.setScale(2, java.math.RoundingMode.HALF_UP).toString());
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
                        Table settTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1, 1 }))
                                        .useAllAvailableWidth();
                        settTable.addHeaderCell("From");
                        settTable.addHeaderCell("Action");
                        settTable.addHeaderCell("To");
                        settTable.addHeaderCell("Amount");

                        for (TransactionDto t : settlements) {
                                settTable.addCell(userNames.get(t.getFrom()));
                                settTable.addCell("owes");
                                settTable.addCell(userNames.get(t.getTo()));
                                String currencyCode = appConfig.getCurrencyCode();
                                String symbol = appConfig.getSymbol(currencyCode);
                                settTable.addCell(symbol + " " + t.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toString() + " " + currencyCode);
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

                for (User member : group.getMembers()) {
                        for (Expense e : expenses) {
                                for (com.malcolm.expensesplitter.models.ExpenseSplit split : e.getSplits()) {
                                        if (split.getUser().getId().equals(member.getId())) {
                                                splitsTable.addCell(member.getName());
                                                splitsTable.addCell(e.getDescription());
                                                splitsTable.addCell(e.getCurrency() + " " + split.getOwedAmount()
                                                                .setScale(2, java.math.RoundingMode.HALF_UP)
                                                                .toString());
                                                splitsTable.addCell(e.getCurrency() + " " + split.getPaidAmount()
                                                                .setScale(2, java.math.RoundingMode.HALF_UP)
                                                                .toString());

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
                // Headers
                writer.println("EXPENSE SPLITTER REPORT");
                writer.println("Group Name," + escapeCsv(group.getName()));
                writer.println("Total Members," + group.getMembers().size());
                writer.println("Generated At," + java.time.LocalDateTime.now().toString());
                writer.println();

                // 1. Expenses Summary Table
                writer.println("--- EXPENSE SUMMARY ---");
                writer.println("Date,Description,Category,Total Amount,Paid By,Payment Mode,Status,Involved Members (Shares)");
                for (Expense e : expenses) {
                        String status = e.getSplits().stream().allMatch(s -> s.isPaid()) ? "Settled" : "Pending";
                        String involved = e.getSplits().stream()
                                        .map(s -> s.getUser().getName() + (s.isPaid() ? " [Settled]" : "") + ": "
                                                        + s.getOwedAmount().setScale(2, java.math.RoundingMode.HALF_UP))
                                        .collect(Collectors.joining(" | "));

                        String paidByStr = e.getPayments().stream()
                                        .map(p -> p.getUser().getName() + " ("
                                                        + e.getCurrency() + " "
                                                        + p.getAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                                                        + ")")
                                        .collect(Collectors.joining(" | "));

                        writer.println(e.getCreatedAt().toString().substring(0, 10) + "," +
                                        escapeCsv(e.getDescription()) + "," +
                                        escapeCsv(e.getCategory() != null ? e.getCategory() : "Other") + "," +
                                        e.getAmount().setScale(2, java.math.RoundingMode.HALF_UP) + " " + e.getCurrency() + "," +
                                        escapeCsv(paidByStr) + "," +
                                        escapeCsv(e.getPaymentMode() != null ? e.getPaymentMode() : "Cash") + "," +
                                        status + "," +
                                        escapeCsv(involved));
                }
                writer.println();

                // 2. Detailed Splits Table
                writer.println("--- DETAILED INDIVIDUAL SPLITS ---");
                writer.println("Member Name,Expense Description,Owed Amount,Paid Amount,Status");
                for (User member : group.getMembers()) {
                        for (Expense e : expenses) {
                                for (com.malcolm.expensesplitter.models.ExpenseSplit split : e.getSplits()) {
                                        if (split.getUser().getId().equals(member.getId())) {
                                                String splitStatus = split.isPaid() ? "Settled" : "Pending";
                                                writer.println(escapeCsv(member.getName()) + "," +
                                                                escapeCsv(e.getDescription()) + "," +
                                                                e.getCurrency() + " " + split.getOwedAmount().setScale(2,
                                                                                java.math.RoundingMode.HALF_UP)
                                                                + ","
                                                                +
                                                                e.getCurrency() + " " + split.getPaidAmount().setScale(2,
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
                for (Expense e : expenses) {
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
                                totalShareOf.put(memberId, totalShareOf.getOrDefault(memberId, BigDecimal.ZERO).add(conv));
                        }
                }

                for (User member : group.getMembers()) {
                        BigDecimal paid = totalPaidBy.get(member.getId());
                        BigDecimal share = totalShareOf.get(member.getId());
                        BigDecimal balance = paid.subtract(share);
                        writer.println(escapeCsv(member.getName()) + "," +
                                        paid.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode + "," +
                                        share.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode + "," +
                                        balance.setScale(2, java.math.RoundingMode.HALF_UP) + " " + prefCode);
                }
                writer.println();

                // 4. Suggested Settlements Section
                writer.println("--- SUGGESTED SETTLEMENTS (TO CLEAR DEBTS) ---");
                writer.println("From,Action,To,Amount");
                if (settlements.isEmpty()) {
                        writer.println("All settled up!,,,");
                } else {
                        for (TransactionDto t : settlements) {
                                writer.println(escapeCsv(userNames.get(t.getFrom())) + ",owes," +
                                                escapeCsv(userNames.get(t.getTo())) + "," +
                                                appConfig.getCurrencyCode() + " " +
                                                t.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
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
