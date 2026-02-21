package com.malcolm.expensesplitter.dao;

import com.malcolm.expensesplitter.db.Database;
import com.malcolm.expensesplitter.model.Expense;

import java.sql.*;
import java.util.List;

public class ExpenseDao {

    public Expense addExpenseWithEqualSplit(Expense expense, List<Integer> memberIds) {
        String insertExpense = "INSERT INTO expenses (group_id, payer_id, description, amount, date) VALUES (?, ?, ?, ?, ?)";
        String insertShare   = "INSERT INTO expense_shares (expense_id, member_id, share_amount) VALUES (?, ?, ?)";

        Connection conn = null;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // transaction

            // 1. Insert expense
            int expenseId;
            try (PreparedStatement ps = conn.prepareStatement(insertExpense, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, expense.getGroupId());
                ps.setInt(2, expense.getPayerId());
                ps.setString(3, expense.getDescription());
                ps.setDouble(4, expense.getAmount());
                ps.setString(5, expense.getDate().toString());
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    conn.rollback();
                    return null;
                }
                expenseId = rs.getInt(1);
            }

            // 2. Equal split among all members
            double share = expense.getAmount() / memberIds.size();

            try (PreparedStatement psShare = conn.prepareStatement(insertShare)) {
                for (Integer memberId : memberIds) {
                    psShare.setInt(1, expenseId);
                    psShare.setInt(2, memberId);
                    psShare.setDouble(3, share);
                    psShare.addBatch();
                }
                psShare.executeBatch();
            }

            conn.commit();

            return new Expense(expenseId,
                    expense.getGroupId(),
                    expense.getPayerId(),
                    expense.getDescription(),
                    expense.getAmount(),
                    expense.getDate());

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
    
}
