package com.malcolm.expensesplitter.dao;

import com.malcolm.expensesplitter.db.Database;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class BalanceDao {

    public static class Balance {
        public String memberName;
        public double paid;
        public double owed;
        public double net;

        public Balance(String memberName, double paid, double owed) {
            this.memberName = memberName;
            this.paid = paid;
            this.owed = owed;
            this.net = paid - owed;
        }
    }

    public Map<Integer, Balance> getBalancesByGroup(int groupId) {
        Map<Integer, Balance> map = new LinkedHashMap<>();

        String paidSql = """
            SELECT m.id AS member_id, m.name, COALESCE(SUM(e.amount), 0) AS paid
            FROM members m
            LEFT JOIN expenses e ON e.payer_id = m.id
            WHERE m.group_id = ?
            GROUP BY m.id, m.name
            """;

        String owedSql = """
            SELECT m.id AS member_id, COALESCE(SUM(es.share_amount), 0) AS owed
            FROM members m
            LEFT JOIN expense_shares es ON es.member_id = m.id
            WHERE m.group_id = ?
            GROUP BY m.id
            """;

        try (Connection conn = Database.getConnection()) {
            // Paid
            try (PreparedStatement ps = conn.prepareStatement(paidSql)) {
                ps.setInt(1, groupId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("member_id");
                    String name = rs.getString("name");
                    double paid = rs.getDouble("paid");
                    map.put(id, new Balance(name, paid, 0.0));
                }
            }

            // Owed
            try (PreparedStatement ps = conn.prepareStatement(owedSql)) {
                ps.setInt(1, groupId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("member_id");
                    double owed = rs.getDouble("owed");
                    Balance b = map.get(id);
                    if (b != null) {
                        b.owed = owed;
                        b.net = b.paid - b.owed;
                    } else {
                        map.put(id, new Balance("Unknown", 0.0, owed));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }
}
