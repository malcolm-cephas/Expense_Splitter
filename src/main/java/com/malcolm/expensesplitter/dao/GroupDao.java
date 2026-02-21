package com.malcolm.expensesplitter.dao;

import com.malcolm.expensesplitter.db.Database;
import com.malcolm.expensesplitter.model.Group;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDao {

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT id, name FROM groups ORDER BY name";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(new Group(
                        rs.getInt("id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return groups;
    }

    public Group addGroup(String name) {
        String sql = "INSERT INTO groups(name) VALUES (?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return new Group(id, name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
