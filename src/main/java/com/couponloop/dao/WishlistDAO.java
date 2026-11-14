package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.Coupon;
import com.couponloop.model.Wishlist;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistDAO {

    public boolean add(int userId, int couponId) throws SQLException {
        String sql = "INSERT IGNORE INTO wishlist (user_id, coupon_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean remove(int userId, int couponId) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isInWishlist(int userId, int couponId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM wishlist WHERE user_id = ? AND coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, couponId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<Wishlist> getWishlistByUser(int userId) throws SQLException {
        List<Wishlist> list = new ArrayList<>();
        String sql = "SELECT w.*, cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name " +
                "FROM wishlist w " +
                "JOIN coupons cp ON w.coupon_id = cp.coupon_id " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN users u ON cp.seller_id = u.user_id " +
                "WHERE w.user_id = ? " +
                "ORDER BY w.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Wishlist w = new Wishlist();
                    w.setWishlistId(rs.getInt("wishlist_id"));
                    w.setUserId(rs.getInt("user_id"));
                    w.setCouponId(rs.getInt("coupon_id"));
                    w.setCreatedAt(rs.getTimestamp("created_at"));

                    Coupon cp = new Coupon();
                    cp.setCouponId(rs.getInt("coupon_id"));
                    cp.setSellerId(rs.getInt("seller_id"));
                    cp.setCategoryId(rs.getInt("category_id"));
                    cp.setCategoryName(rs.getString("category_name"));
                    cp.setCategorySlug(rs.getString("category_slug"));
                    cp.setSellerName(rs.getString("seller_name"));
                    cp.setTitle(rs.getString("title"));
                    cp.setBrandName(rs.getString("brand_name"));
                    cp.setSourcePlatform(rs.getString("source_platform"));
                    cp.setCouponCode("••••••••"); // Masked
                    cp.setDescription(rs.getString("description"));
                    cp.setOriginalValue(rs.getDouble("original_value"));
                    cp.setSellingPrice(rs.getDouble("selling_price"));
                    cp.setMinPurchaseAmount(rs.getDouble("min_purchase_amount"));
                    cp.setExpiryDate(rs.getDate("expiry_date"));
                    cp.setImageUrl(rs.getString("image_url"));
                    cp.setStatus(rs.getString("status"));
                    cp.setInWishlist(true);

                    w.setCoupon(cp);
                    list.add(w);
                }
            }
        }
        return list;
    }
}
