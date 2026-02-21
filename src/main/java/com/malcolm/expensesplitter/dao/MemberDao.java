package com.malcolm.expensesplitter.dao;

import com.malcolm.expensesplitter.db.Database;
import com.malcolm.expensesplitter.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDao {

    public List<Member> getMembersByGroup(int groupId) {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, group_id, name FROM members WHERE group_id = ? ORDER BY name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                members.add(new Member(
                        rs.getInt("id"),
                        rs.getInt("group_id"),
                        rs.getString("name")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return members;
    }

    public Member addMember(int groupId, String name) {
        String sql = "INSERT INTO members(group_id, name) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, groupId);
            ps.setString(2, name);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return new Member(id, groupId, name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
