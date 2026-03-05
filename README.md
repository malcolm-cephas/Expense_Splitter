# 💸 Expense Splitter Pro

[![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?logo=java)](https://openjfx.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Expense Splitter Pro** is a sophisticated desktop application designed to take the headache out of group finances. Whether it's a weekend trip, a shared apartment, or a dinner with friends, this tool ensures everyone pays their fair share with minimal friction.

---

# ✨ Key Features

### 👥 Effortless Group Management
Organize your social circles. Create custom groups for different occasions and manage members seamlessly.

### 💰 Smart Expense Tracking
Log expenses as they happen with flexible split types:

- **Equal**
- **Exact Amount**
- **Percentage**
- **Shares**

### 💳 Multiple Payer Support
A single expense can be split among multiple payers.

Example:  
3 friends split a hotel bill but 2 people paid.

The system records **who paid and who owes** correctly.

### 📉 Debt Simplification Algorithm
Our **Smart Settle Up** feature uses a **greedy optimization algorithm** to minimize the number of transactions needed to settle all debts.

### 📄 Professional Export Options
Generate detailed reports:

- **PDF Reports (iText)**
- **CSV Data Export**

### 🎨 Premium UI/UX
Built with **JavaFX + AtlantaFX (Primer Dark)** for a modern dark-mode experience.

### 💾 Robust Persistence
Uses **Spring Data JPA with an embedded H2 database** for reliable local storage.

---

# 🛠️ Tech Stack

| Layer | Technology |
|------|-------------|
| Core Framework | Spring Boot 3 |
| UI Framework | JavaFX 21 + FXML |
| Styling | AtlantaFX |
| Persistence | Spring Data JPA / Hibernate |
| Database | H2 Embedded Database |
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

### Build the Project

```bash
mvn clean install -DskipTests
```

### Run the Application

```bash
mvn javafx:run
```

---

# 🧩 System Architecture

The application runs as a **single JVM desktop application** combining **JavaFX UI** with a **Spring Boot backend**.

## High Level Architecture

```mermaid
flowchart TD

subgraph System_Context
U[End User]
APP["Expense Splitter Pro\n(JavaFX + Spring Boot)"]
FS[(Local Filesystem)]
H2[(Embedded H2 Database)]
end

U -->|uses| APP
APP -->|reads/writes| H2
APP -->|exports reports| FS

subgraph JVM_Process
BOOT[Application Bootstrap]
CFG[Spring Configuration]

subgraph JavaFX_UI
FXML[FXML Views]
CSS[CSS Styles]
CTRLS[Controllers]
end

subgraph Spring_Backend
SRV[Business Services]
REPO[Repositories]
JPA[JPA Hibernate]
end
end

BOOT --> CFG
BOOT --> FXML
BOOT --> SRV

FXML --> CTRLS
CSS --> FXML

CTRLS --> SRV
SRV --> REPO
REPO --> JPA
JPA --> H2
SRV --> FS
```

---

# 🗄️ Database Model

The application uses an **embedded relational model**.

```mermaid
erDiagram

GROUP ||--o{ EXPENSE : has
GROUP }o--o{ USER : members
EXPENSE ||--o{ EXPENSE_PAYMENT : has
EXPENSE ||--o{ EXPENSE_SPLIT : has
USER ||--o{ EXPENSE_PAYMENT : pays
USER ||--o{ EXPENSE_SPLIT : owes
GROUP ||--o{ SETTLEMENT : has
USER ||--o{ SETTLEMENT : from_to

GROUP {
  long id
  string name
}

USER {
  long id
  string name
}

EXPENSE {
  long id
  string description
  decimal amount
  string splitType
}

EXPENSE_PAYMENT {
  long id
  decimal amountPaid
}

EXPENSE_SPLIT {
  long id
  decimal amountOwed
}

SETTLEMENT {
  long id
  decimal amount
  string status
}
```

---

# 🔄 Application Flow

## Adding an Expense

```mermaid
sequenceDiagram
actor User
participant UI as AddExpenseController
participant Service as ExpenseService
participant DB as H2 Database

User ->> UI: Enter expense
UI ->> Service: addExpense()

Service ->> Service: compute splits

Service ->> DB: save Expense
DB -->> Service: OK

Service -->> UI: success
UI -->> User: UI updated
```

---

## Smart Settle Up Algorithm

```mermaid
sequenceDiagram
actor User
participant UI as SettleUpController
participant Service as SettlementService
participant DB as H2 Database

User ->> UI: Smart Settle Up
UI ->> Service: computeSettlements()

Service ->> DB: load expenses
DB -->> Service: expenses

Service ->> Service: greedy debt algorithm

Service -->> UI: settlement list
UI -->> User: show suggestions
```

---

## Export Reports

```mermaid
sequenceDiagram
actor User
participant UI as ExpenseDetailsController
participant Service as ExportService
participant PDF as iText
participant CSV as CSV Writer
participant FS as Filesystem

User ->> UI: Export report
UI ->> Service: export()

alt PDF
Service ->> PDF: generate
PDF -->> Service: pdf
Service ->> FS: write pdf
else CSV
Service ->> CSV: write rows
CSV -->> Service: csv
Service ->> FS: write csv
end

Service -->> UI: path
UI -->> User: success
```

---

# 📂 Project Structure

```
src/main/java/com/malcolm/expensesplitter
│
├── controllers
│   ├── DashboardController
│   ├── GroupViewController
│   ├── AddExpenseController
│   ├── ExpenseDetailsController
│   ├── SettleUpController
│   └── StatisticsController
│
├── services
│   ├── ExpenseService
│   ├── SettlementService
│   ├── ExportService
│   └── GroupService
│
├── repositories
│   ├── GroupRepository
│   ├── UserRepository
│   ├── ExpenseRepository
│   ├── ExpensePaymentRepository
│   ├── ExpenseSplitRepository
│   └── SettlementRepository
│
└── models
    ├── Group
    ├── User
    ├── Expense
    ├── ExpensePayment
    ├── ExpenseSplit
    ├── Settlement
    ├── SplitType
    └── SettlementStatus
```

---

# 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch

```
git checkout -b feature/AmazingFeature
```

3. Commit your changes

```
git commit -m "Add AmazingFeature"
```

4. Push to your branch

```
git push origin feature/AmazingFeature
```

5. Open a Pull Request

---

# 📄 License

This project is licensed under the **MIT License**.

See the [LICENSE](LICENSE) file for details.

---

# 👨‍💻 Author

*Malcolm Cephas*
- GitHub: [@malcolm-cephas](https://github.com/malcolm-cephas)
