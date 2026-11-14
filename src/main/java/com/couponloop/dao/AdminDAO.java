package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.Coupon;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDAO {

    public Map<String, Object> getPlatformOverview() throws SQLException {
        Map<String, Object> map = new HashMap<>();

        // User stats
        String userSql = "SELECT " +
                "COUNT(*) as total_users, " +
                "SUM(CASE WHEN role = 'SELLER' THEN 1 ELSE 0 END) as total_sellers, " +
                "SUM(CASE WHEN role = 'BUYER' THEN 1 ELSE 0 END) as total_buyers " +
                "FROM users";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(userSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("totalUsers", rs.getInt("total_users"));
                map.put("totalSellers", rs.getInt("total_sellers"));
                map.put("totalBuyers", rs.getInt("total_buyers"));
            }
        }

        // Coupon stats
        String couponSql = "SELECT " +
                "COUNT(*) as total_listings, " +
                "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_approvals, " +
                "SUM(CASE WHEN status IN ('APPROVED', 'ACTIVE') AND expiry_date >= CURRENT_DATE THEN 1 ELSE 0 END) as active_coupons, " +
                "SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_coupons, " +
                "SUM(CASE WHEN status = 'SOLD' THEN 1 ELSE 0 END) as sold_coupons, " +
                "SUM(CASE WHEN status = 'EXPIRED' OR (status IN ('APPROVED', 'ACTIVE') AND expiry_date < CURRENT_DATE) THEN 1 ELSE 0 END) as expired_coupons " +
                "FROM coupons";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(couponSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("totalListings", rs.getInt("total_listings"));
                map.put("pendingApprovals", rs.getInt("pending_approvals"));
                map.put("activeCoupons", rs.getInt("active_coupons"));
                map.put("rejectedCoupons", rs.getInt("rejected_coupons"));
                map.put("soldCoupons", rs.getInt("sold_coupons"));
                map.put("expiredCoupons", rs.getInt("expired_coupons"));
            }
        }

        // Financial stats
        String financeSql = "SELECT " +
                "COALESCE(SUM(amount), 0.0) as total_volume, " +
                "COALESCE(SUM(se.commission_amount), 0.0) as total_commission " +
                "FROM payments p " +
                "LEFT JOIN seller_earnings se ON p.payment_id = (SELECT pur.payment_id FROM purchases pur WHERE pur.purchase_id = se.purchase_id) " +
                "WHERE p.payment_status = 'SUCCESS'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(financeSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("totalVolume", rs.getDouble("total_volume"));
                map.put("totalCommission", rs.getDouble("total_commission"));
            }
        }

        // Pending disputes count
        String disputeSql = "SELECT COUNT(*) as pending_disputes FROM disputes WHERE status = 'OPEN' OR status = 'UNDER_REVIEW'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(disputeSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("pendingDisputes", rs.getInt("pending_disputes"));
            }
        }

        return map;
    }

    public List<Coupon> getAllCoupons(String statusFilter) throws SQLException {
        List<Coupon> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, 5.0 as seller_rating ")
                .append("FROM coupons cp ")
                .append("JOIN coupon_categories c ON cp.category_id = c.category_id ")
                .append("JOIN users u ON cp.seller_id = u.user_id ");

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("WHERE cp.status = ? ");
        }
        sql.append("ORDER BY cp.created_at DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(1, statusFilter.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Coupon c = new Coupon();
                    c.setCouponId(rs.getInt("coupon_id"));
                    c.setSellerId(rs.getInt("seller_id"));
                    c.setSellerName(rs.getString("seller_name"));
                    c.setSellerEmail(rs.getString("seller_email"));
                    c.setCategoryId(rs.getInt("category_id"));
                    c.setCategoryName(rs.getString("category_name"));
                    c.setCategorySlug(rs.getString("category_slug"));
                    c.setTitle(rs.getString("title"));
                    c.setBrandName(rs.getString("brand_name"));
                    c.setSourcePlatform(rs.getString("source_platform"));
                    c.setCouponCode(rs.getString("coupon_code")); // Admin can inspect raw codes
                    c.setDescription(rs.getString("description"));
                    c.setOriginalValue(rs.getDouble("original_value"));
                    c.setSellingPrice(rs.getDouble("selling_price"));
                    c.setMinPurchaseAmount(rs.getDouble("min_purchase_amount"));
                    c.setExpiryDate(rs.getDate("expiry_date"));
                    c.setUsageConditions(rs.getString("usage_conditions"));
                    c.setImageUrl(rs.getString("image_url"));
                    c.setTransferableConfirmed(rs.getBoolean("is_transferable_confirmed"));
                    c.setTermsConfirmed(rs.getBoolean("terms_confirmed"));
                    c.setStatus(rs.getString("status"));
                    try {
                        int adm = rs.getInt("admin_id");
                        if (!rs.wasNull()) c.setAdminId(adm);
                    } catch (SQLException ignored) {}
                    try {
                        c.setApprovedAt(rs.getTimestamp("approved_at"));
                    } catch (SQLException ignored) {}
                    try {
                        c.setRejectionReason(rs.getString("rejection_reason"));
                    } catch (SQLException ignored) {}
                    c.setViewsCount(rs.getInt("views_count"));
                    c.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(c);
                }
            }
        }
        return list;
    }

    public boolean approveCoupon(int couponId, int adminId) throws SQLException {
        String sql = "UPDATE coupons SET status = 'APPROVED', admin_id = ?, approved_at = CURRENT_TIMESTAMP, rejection_reason = NULL WHERE coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean rejectCoupon(int couponId, int adminId, String reason) throws SQLException {
        String sql = "UPDATE coupons SET status = 'REJECTED', admin_id = ?, approved_at = CURRENT_TIMESTAMP, rejection_reason = ? WHERE coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, reason);
            ps.setInt(3, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateCouponStatus(int couponId, String status) throws SQLException {
        String sql = "UPDATE coupons SET status = ? WHERE coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.trim().toUpperCase());
            ps.setInt(2, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> getAllTransactions() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.transaction_id, p.amount, p.payment_method, p.payment_status, p.created_at, " +
                "u.full_name as buyer_name, u.email as buyer_email, " +
                "cp.title as coupon_title, cp.brand_name, " +
                "se.commission_amount, se.net_earning, s.full_name as seller_name " +
                "FROM payments p " +
                "JOIN users u ON p.user_id = u.user_id " +
                "LEFT JOIN purchases pur ON p.payment_id = pur.payment_id " +
                "LEFT JOIN coupons cp ON pur.coupon_id = cp.coupon_id " +
                "LEFT JOIN seller_earnings se ON pur.purchase_id = se.purchase_id " +
                "LEFT JOIN users s ON se.seller_id = s.user_id " +
                "ORDER BY p.created_at DESC LIMIT 50";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("paymentId", rs.getInt("payment_id"));
                map.put("transactionId", rs.getString("transaction_id"));
                map.put("amount", rs.getDouble("amount"));
                map.put("paymentMethod", rs.getString("payment_method"));
                map.put("paymentStatus", rs.getString("payment_status"));
                map.put("createdAt", rs.getTimestamp("created_at"));
                map.put("buyerName", rs.getString("buyer_name"));
                map.put("buyerEmail", rs.getString("buyer_email"));
                map.put("couponTitle", rs.getString("coupon_title"));
                map.put("brandName", rs.getString("brand_name"));
                map.put("commissionAmount", rs.getDouble("commission_amount"));
                map.put("netEarning", rs.getDouble("net_earning"));
                map.put("sellerName", rs.getString("seller_name"));
                list.add(map);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getCategoryDistribution() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.name, COUNT(cp.coupon_id) as count " +
                "FROM coupon_categories c " +
                "LEFT JOIN coupons cp ON c.category_id = cp.category_id " +
                "GROUP BY c.category_id ORDER BY count DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("category", rs.getString("name"));
                map.put("count", rs.getInt("count"));
                list.add(map);
            }
        }
        return list;
    }

    public void recordAction(int adminId, String actionType, String targetType, int targetId, String details) {
        String sql = "INSERT INTO admin_actions (admin_id, action_type, target_type, target_id, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, actionType);
            ps.setString(3, targetType);
            ps.setInt(4, targetId);
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
