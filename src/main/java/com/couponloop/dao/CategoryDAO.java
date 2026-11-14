package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> findAllActive() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.*, COUNT(cp.coupon_id) as coupon_count " +
                "FROM coupon_categories c " +
                "LEFT JOIN coupons cp ON c.category_id = cp.category_id AND cp.status IN ('APPROVED', 'ACTIVE') AND cp.expiry_date >= CURRENT_DATE " +
                "WHERE c.is_active = TRUE " +
                "GROUP BY c.category_id " +
                "ORDER BY c.category_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category cat = mapRow(rs);
                cat.setCouponCount(rs.getInt("coupon_count"));
                list.add(cat);
            }
        }
        return list;
    }

    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.*, COUNT(cp.coupon_id) as coupon_count " +
                "FROM coupon_categories c " +
                "LEFT JOIN coupons cp ON c.category_id = cp.category_id " +
                "GROUP BY c.category_id " +
                "ORDER BY c.category_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category cat = mapRow(rs);
                cat.setCouponCount(rs.getInt("coupon_count"));
                list.add(cat);
            }
        }
        return list;
    }

    public Category findById(int categoryId) throws SQLException {
        String sql = "SELECT * FROM coupon_categories WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Category findBySlug(String slug) throws SQLException {
        String sql = "SELECT * FROM coupon_categories WHERE slug = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(Category c) throws SQLException {
        String sql = "INSERT INTO coupon_categories (name, slug, description, icon_class, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getSlug());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getIconClass());
            ps.setBoolean(5, c.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Category c) throws SQLException {
        String sql = "UPDATE coupon_categories SET name = ?, slug = ?, description = ?, icon_class = ?, is_active = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getSlug());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getIconClass());
            ps.setBoolean(5, c.isActive());
            ps.setInt(6, c.getCategoryId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean toggleActive(int categoryId, boolean active) throws SQLException {
        String sql = "UPDATE coupon_categories SET is_active = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, categoryId);
            return ps.executeUpdate() > 0;
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getInt("category_id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        c.setDescription(rs.getString("description"));
        c.setIconClass(rs.getString("icon_class"));
        c.setActive(rs.getBoolean("is_active"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
