package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO {

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, mobile, password_hash, salt, role, status, upi_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getMobile());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getSalt());
            ps.setString(6, user.getRole() != null ? user.getRole() : "BUYER");
            ps.setString(7, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            ps.setString(8, user.getUpiId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean updateProfile(int userId, String fullName, String mobile) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, mobile = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, mobile);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateUpiId(int userId, String upiId) throws SQLException {
        String sql = "UPDATE users SET upi_id = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, upiId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int userId, String hash, String salt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int userId, String status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateRole(int userId, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> findAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Map<String, Integer> getUserCounts() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN role = 'SELLER' THEN 1 ELSE 0 END) as sellers, " +
                "SUM(CASE WHEN role = 'BUYER' THEN 1 ELSE 0 END) as buyers, " +
                "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_users, " +
                "SUM(CASE WHEN status = 'SUSPENDED' THEN 1 ELSE 0 END) as suspended_users " +
                "FROM users";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("total", rs.getInt("total"));
                map.put("sellers", rs.getInt("sellers"));
                map.put("buyers", rs.getInt("buyers"));
                map.put("active", rs.getInt("active_users"));
                map.put("suspended", rs.getInt("suspended_users"));
            }
        }
        return map;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setMobile(rs.getString("mobile"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        u.setUpiId(rs.getString("upi_id"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }
}
