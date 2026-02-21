package com.malcolm.expensesplitter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    // This is the SQLite file that will be created in your project root folder
    private static final String URL = "jdbc:sqlite:expense_splitter.db";

    // Static block: runs ONE time when the class is first loaded
    static {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create groups table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL
                );
            """);

            // Create members table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    FOREIGN KEY (group_id) REFERENCES groups(id)
                );
            """);

            // Create expenses table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    payer_id INTEGER NOT NULL,
                    description TEXT,
                    amount REAL NOT NULL,
                    date TEXT NOT NULL,
                    FOREIGN KEY (group_id) REFERENCES groups(id),
                    FOREIGN KEY (payer_id) REFERENCES members(id)
                );
            """);

            // Create expense_shares table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS expense_shares (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expense_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    share_amount REAL NOT NULL,
                    FOREIGN KEY (expense_id) REFERENCES expenses(id),
                    FOREIGN KEY (member_id) REFERENCES members(id)
                );
            """);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Called whenever we need a DB connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
