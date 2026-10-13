Expense Splitter patch - 13 Oct 2026

Files included:
- src/main/java/com/malcolm/expensesplitter/services/SettlementService.java
- src/main/java/com/malcolm/expensesplitter/services/ExportService.java

Apply by extracting this ZIP into the ROOT of your existing expense-splitter project and choosing Replace/Overwrite for these two files only. Do not replace your data/ folder or the whole project.

Changes:
1. Standard expense settlement now respects each split's paidAmount, including overpayments, and distributes the remaining net amount to the expense payer(s). This should account for the shown example: Malcolm paid 5000 against a 2500 share, while Teena, Swetha and Medhansh remain short.
2. PDF, CSV and JSON export reload the saved group within a read-only transaction and initialize lazy business/investment collections, addressing Group.businesses LazyInitializationException.

Verification note: Maven is not installed in the patching environment, so `mvn test` could not be run here. Run `mvn test` locally after applying the patch.
