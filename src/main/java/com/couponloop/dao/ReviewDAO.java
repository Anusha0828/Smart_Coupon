package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public boolean create(Review review) throws SQLException {
        String sql = "INSERT INTO reviews (purchase_id, buyer_id, seller_id, rating, review_text) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getPurchaseId());
            ps.setInt(2, review.getBuyerId());
            ps.setInt(3, review.getSellerId());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getReviewText());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Review> findBySellerId(int sellerId) throws SQLException {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT r.*, u.full_name as buyer_name " +
                "FROM reviews r " +
                "JOIN users u ON r.buyer_id = u.user_id " +
                "WHERE r.seller_id = ? " +
                "ORDER BY r.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setReviewId(rs.getInt("review_id"));
                    r.setPurchaseId(rs.getInt("purchase_id"));
                    r.setBuyerId(rs.getInt("buyer_id"));
                    r.setBuyerName(rs.getString("buyer_name"));
                    r.setSellerId(rs.getInt("seller_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setReviewText(rs.getString("review_text"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public Review findByPurchaseId(int purchaseId) throws SQLException {
        String sql = "SELECT r.*, u.full_name as buyer_name FROM reviews r JOIN users u ON r.buyer_id = u.user_id WHERE r.purchase_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Review r = new Review();
                    r.setReviewId(rs.getInt("review_id"));
                    r.setPurchaseId(rs.getInt("purchase_id"));
                    r.setBuyerId(rs.getInt("buyer_id"));
                    r.setBuyerName(rs.getString("buyer_name"));
                    r.setSellerId(rs.getInt("seller_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setReviewText(rs.getString("review_text"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    return r;
                }
            }
        }
        return null;
    }

    public double getSellerAverageRating(int sellerId) throws SQLException {
        String sql = "SELECT AVG(rating) as avg_rating FROM reviews WHERE seller_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_rating");
                    return rs.wasNull() ? 5.0 : Math.round(avg * 10.0) / 10.0;
                }
            }
        }
        return 5.0;
    }
}
