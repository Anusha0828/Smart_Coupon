package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.SellerEarning;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerEarningDAO {

    public Map<String, Object> getSellerDashboardStats(int sellerId) throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        String sqlCoupons = "SELECT " +
                "COUNT(*) as total_uploaded, " +
                "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_listings, " +
                "SUM(CASE WHEN status IN ('APPROVED', 'ACTIVE') AND expiry_date >= CURRENT_DATE THEN 1 ELSE 0 END) as active_listings, " +
                "SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_listings, " +
                "SUM(CASE WHEN status = 'SOLD' THEN 1 ELSE 0 END) as coupons_sold, " +
                "SUM(CASE WHEN status = 'EXPIRED' OR (status IN ('APPROVED', 'ACTIVE') AND expiry_date < CURRENT_DATE) THEN 1 ELSE 0 END) as expired_listings " +
                "FROM coupons WHERE seller_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCoupons)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalUploaded", rs.getInt("total_uploaded"));
                    stats.put("pendingListings", rs.getInt("pending_listings"));
                    stats.put("activeListings", rs.getInt("active_listings"));
                    stats.put("rejectedListings", rs.getInt("rejected_listings"));
                    stats.put("couponsSold", rs.getInt("coupons_sold"));
                    stats.put("expiredListings", rs.getInt("expired_listings"));
                }
            }
        }

        String sqlEarnings = "SELECT " +
                "COALESCE(SUM(gross_amount), 0.0) as total_gross, " +
                "COALESCE(SUM(commission_amount), 0.0) as total_commission, " +
                "COALESCE(SUM(net_earning), 0.0) as total_net_earnings, " +
                "COALESCE(SUM(CASE WHEN payout_status = 'PENDING' THEN net_earning ELSE 0.0 END), 0.0) as pending_payouts, " +
                "COALESCE(SUM(CASE WHEN payout_status = 'PAID' THEN net_earning ELSE 0.0 END), 0.0) as paid_payouts " +
                "FROM seller_earnings WHERE seller_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlEarnings)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalGross", rs.getDouble("total_gross"));
                    stats.put("totalCommission", rs.getDouble("total_commission"));
                    stats.put("totalNetEarnings", rs.getDouble("total_net_earnings"));
                    stats.put("pendingPayouts", rs.getDouble("pending_payouts"));
                    stats.put("paidPayouts", rs.getDouble("paid_payouts"));
                }
            }
        }

        return stats;
    }

    public List<SellerEarning> findEarningsBySeller(int sellerId) throws SQLException {
        List<SellerEarning> list = new ArrayList<>();
        String sql = "SELECT se.*, cp.title as coupon_title, cp.brand_name " +
                "FROM seller_earnings se " +
                "JOIN coupons cp ON se.coupon_id = cp.coupon_id " +
                "WHERE se.seller_id = ? " +
                "ORDER BY se.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SellerEarning se = new SellerEarning();
                    se.setEarningId(rs.getInt("earning_id"));
                    se.setSellerId(rs.getInt("seller_id"));
                    se.setCouponId(rs.getInt("coupon_id"));
                    se.setCouponTitle(rs.getString("coupon_title"));
                    se.setBrandName(rs.getString("brand_name"));
                    se.setPurchaseId(rs.getInt("purchase_id"));
                    se.setGrossAmount(rs.getDouble("gross_amount"));
                    se.setCommissionRate(rs.getDouble("commission_rate"));
                    se.setCommissionAmount(rs.getDouble("commission_amount"));
                    se.setNetEarning(rs.getDouble("net_earning"));
                    se.setPayoutStatus(rs.getString("payout_status"));
                    se.setPayoutDate(rs.getTimestamp("payout_date"));
                    se.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(se);
                }
            }
        }
        return list;
    }

    public boolean requestPayout(int sellerId, String upiId) throws SQLException {
        String updateUpi = "UPDATE users SET upi_id = ? WHERE user_id = ?";
        String updateEarnings = "UPDATE seller_earnings SET payout_status = 'PAID', payout_date = CURRENT_TIMESTAMP " +
                "WHERE seller_id = ? AND payout_status = 'PENDING'";
        String notify = "INSERT INTO notifications (user_id, title, message, type) " +
                "VALUES (?, 'Payout Processed', ?, 'SUCCESS')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (upiId != null && !upiId.trim().isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(updateUpi)) {
                        ps.setString(1, upiId.trim());
                        ps.setInt(2, sellerId);
                        ps.executeUpdate();
                    }
                }

                int updatedCount;
                try (PreparedStatement ps = conn.prepareStatement(updateEarnings)) {
                    ps.setInt(1, sellerId);
                    updatedCount = ps.executeUpdate();
                }

                if (updatedCount > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(notify)) {
                        ps.setInt(1, sellerId);
                        ps.setString(2, "Your payout request was safely processed to your registered UPI ID. " + updatedCount + " earnings cleared.");
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
