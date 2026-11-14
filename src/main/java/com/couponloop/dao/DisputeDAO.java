package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.CouponReport;
import com.couponloop.model.Dispute;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisputeDAO {

    public boolean createDispute(Dispute d) throws SQLException {
        String sql = "INSERT INTO disputes (purchase_id, coupon_id, buyer_id, seller_id, reason, details, evidence_url, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN')";
        String updatePur = "UPDATE purchases SET dispute_status = 'RAISED' WHERE purchase_id = ?";
        String notifAdmin = "INSERT INTO notifications (user_id, title, message, type) " +
                "SELECT user_id, 'New Dispute Raised', 'A buyer raised a dispute for coupon #" + d.getCouponId() + "', 'DISPUTE' FROM users WHERE role = 'ADMIN'";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, d.getPurchaseId());
                    ps.setInt(2, d.getCouponId());
                    ps.setInt(3, d.getBuyerId());
                    ps.setInt(4, d.getSellerId());
                    ps.setString(5, d.getReason());
                    ps.setString(6, d.getDetails());
                    ps.setString(7, d.getEvidenceUrl());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(updatePur)) {
                    ps.setInt(1, d.getPurchaseId());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(notifAdmin)) {
                    ps.executeUpdate();
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

    public List<Dispute> findAllDisputes() throws SQLException {
        List<Dispute> list = new ArrayList<>();
        String sql = "SELECT d.*, cp.title as coupon_title, b.full_name as buyer_name, s.full_name as seller_name " +
                "FROM disputes d " +
                "JOIN coupons cp ON d.coupon_id = cp.coupon_id " +
                "JOIN users b ON d.buyer_id = b.user_id " +
                "JOIN users s ON d.seller_id = s.user_id " +
                "ORDER BY d.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDispute(rs));
            }
        }
        return list;
    }

    public List<Dispute> findByBuyer(int buyerId) throws SQLException {
        List<Dispute> list = new ArrayList<>();
        String sql = "SELECT d.*, cp.title as coupon_title, b.full_name as buyer_name, s.full_name as seller_name " +
                "FROM disputes d " +
                "JOIN coupons cp ON d.coupon_id = cp.coupon_id " +
                "JOIN users b ON d.buyer_id = b.user_id " +
                "JOIN users s ON d.seller_id = s.user_id " +
                "WHERE d.buyer_id = ? " +
                "ORDER BY d.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDispute(rs));
                }
            }
        }
        return list;
    }

    public boolean resolveDispute(int disputeId, String resolution, String adminNotes, int adminId) throws SQLException {
        String fetchSql = "SELECT d.*, p.payment_id, p.purchase_price FROM disputes d JOIN purchases p ON d.purchase_id = p.purchase_id WHERE d.dispute_id = ?";
        String updateDispute = "UPDATE disputes SET status = ?, admin_notes = ? WHERE dispute_id = ?";
        String updatePurchase = "UPDATE purchases SET dispute_status = ? WHERE purchase_id = ?";
        String updatePayment = "UPDATE payments SET payment_status = 'REFUNDED' WHERE payment_id = ?";
        String cancelEarning = "UPDATE seller_earnings SET payout_status = 'CANCELLED' WHERE purchase_id = ?";
        String notifyBuyer = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, 'DISPUTE')";
        String notifySeller = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, 'DISPUTE')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int buyerId = 0, sellerId = 0, purchaseId = 0, paymentId = 0;
                double price = 0;
                try (PreparedStatement ps = conn.prepareStatement(fetchSql)) {
                    ps.setInt(1, disputeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            buyerId = rs.getInt("buyer_id");
                            sellerId = rs.getInt("seller_id");
                            purchaseId = rs.getInt("purchase_id");
                            paymentId = rs.getInt("payment_id");
                            price = rs.getDouble("purchase_price");
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                boolean isRefund = "RESOLVED_REFUND".equalsIgnoreCase(resolution);
                String newStatus = isRefund ? "RESOLVED_REFUND" : "RESOLVED_REJECTED";
                String purchaseDisputeStatus = isRefund ? "REFUNDED" : "RESOLVED";

                try (PreparedStatement ps = conn.prepareStatement(updateDispute)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, adminNotes);
                    ps.setInt(3, disputeId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(updatePurchase)) {
                    ps.setString(1, purchaseDisputeStatus);
                    ps.setInt(2, purchaseId);
                    ps.executeUpdate();
                }

                if (isRefund) {
                    try (PreparedStatement ps = conn.prepareStatement(updatePayment)) {
                        ps.setInt(1, paymentId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(cancelEarning)) {
                        ps.setInt(1, purchaseId);
                        ps.executeUpdate();
                    }
                }

                // Notify buyer
                try (PreparedStatement ps = conn.prepareStatement(notifyBuyer)) {
                    ps.setInt(1, buyerId);
                    ps.setString(2, isRefund ? "Dispute Resolved: Refund Approved" : "Dispute Closed");
                    ps.setString(3, isRefund ? String.format("Your refund of ₹%.2f has been approved. Note: %s", price, adminNotes)
                            : "Your dispute request was reviewed and closed: " + adminNotes);
                    ps.executeUpdate();
                }

                // Notify seller
                try (PreparedStatement ps = conn.prepareStatement(notifySeller)) {
                    ps.setInt(1, sellerId);
                    ps.setString(2, isRefund ? "Dispute Notice: Order Refunded" : "Dispute Dismissed");
                    ps.setString(3, isRefund ? "A dispute on your listing resulted in buyer refund: " + adminNotes
                            : "Dispute on your listing was closed in your favor. Your earnings remain intact.");
                    ps.executeUpdate();
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

    public boolean createReport(int couponId, int userId, String reason, String details) throws SQLException {
        String sql = "INSERT INTO coupon_reports (coupon_id, reported_by_user_id, reason, details, status) VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            ps.setString(3, reason);
            ps.setString(4, details);
            return ps.executeUpdate() > 0;
        }
    }

    public List<CouponReport> findAllReports() throws SQLException {
        List<CouponReport> list = new ArrayList<>();
        String sql = "SELECT cr.*, cp.title as coupon_title, u.full_name as reporter_name " +
                "FROM coupon_reports cr " +
                "JOIN coupons cp ON cr.coupon_id = cp.coupon_id " +
                "JOIN users u ON cr.reported_by_user_id = u.user_id " +
                "ORDER BY cr.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CouponReport r = new CouponReport();
                r.setReportId(rs.getInt("report_id"));
                r.setCouponId(rs.getInt("coupon_id"));
                r.setCouponTitle(rs.getString("coupon_title"));
                r.setReportedByUserId(rs.getInt("reported_by_user_id"));
                r.setReporterName(rs.getString("reporter_name"));
                r.setReason(rs.getString("reason"));
                r.setDetails(rs.getString("details"));
                r.setStatus(rs.getString("status"));
                r.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(r);
            }
        }
        return list;
    }

    private Dispute mapDispute(ResultSet rs) throws SQLException {
        Dispute d = new Dispute();
        d.setDisputeId(rs.getInt("dispute_id"));
        d.setPurchaseId(rs.getInt("purchase_id"));
        d.setCouponId(rs.getInt("coupon_id"));
        d.setCouponTitle(rs.getString("coupon_title"));
        d.setBuyerId(rs.getInt("buyer_id"));
        d.setBuyerName(rs.getString("buyer_name"));
        d.setSellerId(rs.getInt("seller_id"));
        d.setSellerName(rs.getString("seller_name"));
        d.setReason(rs.getString("reason"));
        d.setDetails(rs.getString("details"));
        d.setEvidenceUrl(rs.getString("evidence_url"));
        d.setStatus(rs.getString("status"));
        d.setAdminNotes(rs.getString("admin_notes"));
        d.setCreatedAt(rs.getTimestamp("created_at"));
        d.setUpdatedAt(rs.getTimestamp("updated_at"));
        return d;
    }
}
