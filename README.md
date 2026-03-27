# 💸 Expense Splitter Pro

[![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?logo=java)](https://openjfx.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Expense Splitter Pro** is a sophisticated desktop application designed to take the headache out of group finances. Whether it's a weekend trip, a shared apartment, or a dinner with friends, this tool ensures everyone pays their fair share with minimal friction.

---

# 📸 Screenshots

<p align="center">
  <img src="assets/dashboard.png" width="100%" alt="Dashboard" />
  </p>
<p align="center">
  <img src="assets/add_expense.png" width="30%" alt="Add Expense" />
  <img src="assets/settle_up.png" width="30%" alt="Settle Up" />
  <img src="assets/statistics.png" width="30%" alt="Statistics" />
</p>

---

# 📄 Sample Reports

Experience the precision of our multicurrency reporting with these sample exports from an international trip:

- 🇪🇺 [Euro Trip 2024 - Euro Report](assets/Euro_Trip_2024_Euro_Report.pdf)
- 🇮🇳 [Euro Trip 2024 - INR Report](assets/Euro_Trip_2024_INR_Report.pdf)
- 🇺🇸 [Euro Trip 2024 - USD Report](assets/Euro_Trip_2024_USD_Report.pdf)

---

# ✨ Key Features

### 👥 Effortless Group Management
Organize your social circles. Create custom groups for different occasions and manage members seamlessly with automatic user profile setup.

### 💰 Smart Expense Tracking
Log expenses as they happen with flexible split types:
- **Equal**
- **Exact Amount**
- **Percentage**
- **Shares**

### 🌍 Multi-Currency Support
Record expenses in any currency (INR, USD, EUR, etc.) and let the app handle the math.
- **Smart Currency Selection**: A powerful searchable modal allows you to pick from 100+ global currencies by name or country.
- **Advanced Financial Formatting**: Automatically applies Western, Indian (3-2), or East Asian (Myriad) digit grouping based on the currency.
- **Live Exchange Rates**: Real-time conversion using the [Currency API](https://github.com/fawazahmed0/exchange-api).
- **Persistent Offline Cache**: Rates and currency metadata are cached locally (`exchange_rates.json`, `currency_names.json`) for seamless offline operation.
- **Global Normalization**: All statistics and settlements are automatically normalized to your primary currency.

### 💳 Multiple Payer Support
A single expense can be split among multiple payers. The system tracks partial payments and contributions meticulously.

### 📉 Debt Simplification Algorithm
Our **Smart Settle Up** feature uses a **greedy optimization algorithm** to minimize the number of transactions needed to settle all debts across multiple currencies.

### 📄 Professional Export Options
Generate detailed reports with smart naming:
- **PDF Reports (iText)**: Styled summaries with charts. (Default: `GroupName_ExpenseReport`)
- **CSV Data Export**: Raw transaction data for Excel/Sheets. (Default: `GroupName_ExpenseReport`)
- **JSON Backup**: Full database portable export for migration. (Default: `GroupName_Backup`)

### 👨‍👩‍👧‍👦 Family Grouping
Enable family-based expense tracking to aggregate spending by household while maintaining individual member records.

### 🎨 Premium UI/UX
Built with **JavaFX + AtlantaFX (Primer Dark)** for a modern dark-mode experience.

### 💾 Robust Persistence
Uses **Spring Data JPA with an embedded H2 database** for reliable local storage and automatic data migration.

---

# 🛠️ Tech Stack

| Layer | Technology |
|------|-------------|
| Core Framework | Spring Boot 3 |
| UI Framework | JavaFX 21 + FXML |
| Styling | AtlantaFX |
| Persistence | Spring Data JPA / Hibernate |
| Database | H2 Embedded Database |
| JSON Processing | Jackson Databind |
| PDF Generation | iText PDF Core |
| Data Export | CSV Writer |
| Build Tool | Maven |

---

# 🚀 Getting Started

## Prerequisites

- **Java JDK 17+**
- **Maven 3.6+**

---

## Installation

### Clone the Repository

```bash
git clone https://github.com/malcolm-cephas/Expense_Splitter.git
cd Expense_Splitter/expense-splitter
```

### Quick Start (Windows)
To set up the environment and build the project for the first time:
```bash
./setup.bat
```

Once setup is complete, run the application:
```bash
./start.bat
```

> [!NOTE]
> Detailed system dependencies are listed in [requirements.txt](requirements.txt).

## 👷 CI/CD & Deployment
The project includes a robust GitHub Actions workflow ([build.yml](.github/workflows/build.yml)) that:
- Automatically builds and tests the application on every push to `main`.
- Generates an executable JAR as a build artifact.
- Enables seamless release management via GitHub Releases.

### Build the Project manually
```bash
mvn clean install -DskipTests
mvn javafx:run
```

---

# 🧩 System Architecture

The application runs as a **single JVM desktop application** combining **JavaFX UI** with a **Spring Boot backend** and external **Currency API** integration.

## High Level Architecture

```mermaid
flowchart TD

subgraph External
API["Currency API\n(Exchange Rates)"]
end

subgraph System_Context
U[End User]
APP["Expense Splitter Pro\n(JavaFX + Spring Boot)"]
FS[(Local Filesystem)]
H2[(Embedded H2 Database)]
CACHE["exchange_rates.json\n(Offline Cache)"]
BKUP["Exported Backups\n(.json / .csv / .pdf)"]
end

U -->|uses| APP
APP -->|fetches| API
APP -->|reads/writes| H2
APP -->|persist| CACHE
APP -->|exports/imports| BKUP
APP -->|renders| FS

subgraph JVM_Process
BOOT[Application Bootstrap]
CFG[Spring Configuration]

subgraph JavaFX_UI
FXML[FXML Views]
CTRLS[Controllers]
end

subgraph Spring_Backend
SRV[Business Services]
EXSRV[Exchange Rate Service]
REPO[Repositories]
JPA[JPA Hibernate]
end
end

BOOT --> CFG
BOOT --> FXML
BOOT --> SRV

FXML --> CTRLS
CTRLS --> SRV
SRV --> EXSRV
SRV --> REPO
REPO --> JPA
JPA --> H2
SRV --> FS
```

---

# 🗄️ Database Model

The application uses an **embedded relational model** with support for currency preferences.

```mermaid
erDiagram

GROUP ||--o{ EXPENSE : has
GROUP }o--o{ USER : members
EXPENSE ||--o{ EXPENSE_PAYMENT : has
EXPENSE ||--o{ EXPENSE_SPLIT : has
USER ||--o{ EXPENSE_PAYMENT : pays
USER ||--o{ EXPENSE_SPLIT : owes

GROUP {
  UUID id
  string name
  string description
  decimal budget
  string budgetCurrency
  boolean familyGroupingEnabled
}

USER {
  UUID id
  string name
  string email
  string currencyPreference
  string familyName
}

EXPENSE {
  UUID id
  string description
  decimal amount
  string currency
  string splitType
  string category
  LocalDate expenseDate
}

EXPENSE_PAYMENT {
  UUID id
  decimal amount
}

EXPENSE_SPLIT {
  UUID id
  decimal owedAmount
  decimal paidAmount
  boolean isPaid
}
```

---

# � Application Flow

## Adding an Expense
The app fetches live rates (or uses cache) to ensure correct conversion if the expense currency differs from the group base.

```mermaid
sequenceDiagram
actor User
participant UI as AddExpenseController
participant EX as ExchangeRateService
participant Service as ExpenseService
participant DB as H2 Database

User ->> UI: Select Currency & Payers
UI ->> EX: getExchangeRate()
EX -->> UI: Rate (Cached/Live)
UI ->> Service: addExpense(multiple_payers)

Service ->> Service: compute multi-payer splits
Service ->> Service: normalize to base currency

Service ->> DB: save Expense + Payments
DB -->> Service: OK

Service -->> UI: success
UI -->> User: UI updated
```

---

## Smart Settle Up Algorithm
Debts are simplified globally by converting all individual expense balances into the user's primary currency first.

```mermaid
sequenceDiagram
actor User
participant UI as SettleUpController
participant EX as ExchangeRateService
participant Service as SettlementService
participant DB as H2 Database

User ->> UI: Smart Settle Up
UI ->> Service: computeSettlements()

Service ->> DB: load expenses
DB -->> Service: expenses

loop For each Expense
    Service ->> EX: convert to base currency
    EX -->> Service: normalized amount
end

opt Family Grouping Enabled
    Service ->> Service: Aggregate by Family Name
end

Service ->> Service: greedy debt algorithm

Service -->> UI: simplified settlement list
UI -->> User: show suggestions
```

---

## Export Reports
Generate reports that preserve both the original currency and the normalized totals.

```mermaid
sequenceDiagram
actor User
participant UI as GroupViewController
participant Service as ExportService
participant PDF as iText
participant CSV as CSV Writer
participant JSON as Jackson
participant FS as Filesystem

User ->> UI: Export (Choose Type)
UI ->> Service: export()

alt PDF/CSV Report
Service ->> PDF: generate (GroupName_ExpenseReport)
PDF -->> Service: file
else JSON Backup
Service ->> JSON: serialize (GroupName_Backup)
JSON -->> Service: file
end

Service ->> FS: write file
Service -->> UI: path
UI -->> User: success success
```

---

# �📂 Project Structure

```
src/main/java/com/malcolm/expensesplitter
│
├── config
│   └── AppConfig (Global Settings & Currency Symbols)
│
├── controllers
│   ├── DashboardController (Setup & Navigation)
│   ├── GroupViewController (Expense List)
│   ├── AddExpenseController (Multi-Currency Entry)
│   ├── ExpenseDetailsController (Split Breakdown)
│   └── StatisticsController (Normalized spending charts)
│
├── services
│   ├── ExpenseService (Split Logic)
│   ├── SettlementService (Debt Simplification)
│   ├── ExchangeRateService (API & Offline Cache)
│   └── ExportService (PDF/CSV Generation)
│
├── repositories
│   └── ... (JPA Data Access)
│
└── models
    └── ... (Domain Entities)
```

---

# 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to your branch
5. Open a Pull Request

---

# 📄 License

This project is licensed under the **MIT License**.
See the [LICENSE](LICENSE) file for details.

---

# 👨‍💻 Author

*Malcolm Cephas*
- GitHub: [@malcolm-cephas](https://github.com/malcolm-cephas)

---
