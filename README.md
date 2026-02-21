# 💸 Expense Splitter Pro

[![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?logo=java)](https://openjfx.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Expense Splitter Pro** is a sophisticated desktop application designed to take the headache out of group finances. Whether it's a weekend trip, a shared apartment, or a dinner with friends, this tool ensures everyone pays their fair share with minimal friction.

---

## ✨ Key Features

- **👥 Effortless Group Management**: Organize your social circles. Create custom groups for different occasions and manage members seamlessly.
- **💰 Smart Expense Tracking**: Log expenses as they happen. Specify who paid, the total amount, and let the app handle the math.
- **📉 Debt Simplification Algorithm**: Our "Smart Settle Up" feature uses a greedy optimization algorithm to calculate the *minimum* number of transactions required to clear all debts within a group.
- **🎨 Premium UI/UX**: Built with **JavaFX** and styled with the **AtlantaFX** (Primer Dark) theme for a sleek, modern, and dark-mode-first experience.
- **💾 Robust Data Management**: Uses **Spring Data JPA** with an embedded **H2** database for zero-configuration local storage.

---

## 🛠️ Tech Stack

- **Core Framework**: Spring Boot 3.2.3
- **Frontend**: JavaFX 21 + FXML
- **Styling**: AtlantaFX (Modern CSS Framework for JavaFX)
- **Database**: H2 (Embedded SQL)
- **Persistence**: Spring Data JPA / Hibernate
- **Build Tool**: Maven

---

## 🚀 Getting Started

### Prerequisites

- **Java JDK 17** or higher
- **Maven 3.6+**

### Installation & Execution

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/expense-splitter-pro.git
   cd expense-splitter-pro/expense-splitter
   ```

2. **Build the Project**
   ```bash
   mvn clean install -DskipTests
   ```

3. **Run the Application**
   ```bash
   mvn javafx:run
   ```

---

## 🏗️ Architecture Overview

The project follows a clean, decoupled architecture to ensure maintainability and scalability:

- **UI Layer (`controllers/`)**: JavaFX controllers managing user interactions and FXML data binding.
- **Service Layer (`services/`)**: Contains core business logic, including the debt simplification engine.
- **Persistence Layer (`repositories/`)**: Spring Data repositories for abstracting database operations.
- **Model Layer (`models/`)**: JPA entities representing Groups, Users, and Expenses.

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request for any improvements.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed with ❤️ by Malcolm*
