package com.couponloop.dao;

import com.couponloop.config.AppConfig;
import com.couponloop.config.DBConnection;
import com.couponloop.model.Purchase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseDAO {

    public static class PurchaseResult {
        public boolean success;
        public String message;
        public Purchase purchase;

        public PurchaseResult(boolean success, String message, Purchase purchase) {
            this.success = success;
            this.message = message;
            this.purchase = purchase;
        }
    }

    /**
     * Executes atomic purchase transaction using pessimistic row locking.
     * Prevents race conditions, double-spending, and maintains database consistency.
     */
    public PurchaseResult executePurchase(int buyerId, int couponId, String paymentMethod, String demoTxnId) {
        Connection conn = null;
        PreparedStatement psLock = null;
        PreparedStatement psPayment = null;
        PreparedStatement psCouponSold = null;
        PreparedStatement psPurchase = null;
        PreparedStatement psEarning = null;
        PreparedStatement psNotifSeller = null;
        PreparedStatement psNotifBuyer = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Lock coupon row for update
            String lockSql = "SELECT * FROM coupons WHERE coupon_id = ? FOR UPDATE";
            psLock = conn.prepareStatement(lockSql);
            psLock.setInt(1, couponId);
            ResultSet rsCoupon = psLock.executeQuery();

            if (!rsCoupon.next()) {
                conn.rollback();
                return new PurchaseResult(false, "Coupon does not exist.", null);
            }

            String status = rsCoupon.getString("status");
            Date expiryDate = rsCoupon.getDate("expiry_date");
            int sellerId = rsCoupon.getInt("seller_id");
            String couponCode = rsCoupon.getString("coupon_code");
            String couponTitle = rsCoupon.getString("title");
            String brandName = rsCoupon.getString("brand_name");
            double sellingPrice = rsCoupon.getDouble("selling_price");

            if (!"ACTIVE".equalsIgnoreCase(status) && !"APPROVED".equalsIgnoreCase(status)) {
                conn.rollback();
                return new PurchaseResult(false, "This coupon is not available for purchase (pending approval, sold, or expired).", null);
            }

            Date today = new Date(System.currentTimeMillis());
            if (expiryDate.before(today)) {
                conn.rollback();
                return new PurchaseResult(false, "This coupon has expired.", null);
            }

            if (sellerId == buyerId) {
                conn.rollback();
                return new PurchaseResult(false, "You cannot purchase your own coupon listing.", null);
            }

            // 2. Insert into payments table
            String txnId = (demoTxnId != null && !demoTxnId.trim().isEmpty())
                    ? demoTxnId.trim()
                    : "TXN_SANDBOX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            String paymentSql = "INSERT INTO payments (transaction_id, user_id, amount, payment_method, payment_status, gateway_payload) " +
                    "VALUES (?, ?, ?, ?, 'SUCCESS', ?)";
            psPayment = conn.prepareStatement(paymentSql, Statement.RETURN_GENERATED_KEYS);
            psPayment.setString(1, txnId);
            psPayment.setInt(2, buyerId);
            psPayment.setDouble(3, sellingPrice);
            psPayment.setString(4, paymentMethod != null ? paymentMethod : "DEMO_SANDBOX");
            psPayment.setString(5, "{\"gateway\":\"CouponLoop Sandbox Simulator\",\"status\":\"SUCCESS\",\"auth_code\":\"SANDBOX_APPROVED\"}");
            psPayment.executeUpdate();

            int paymentId = -1;
            ResultSet rsPayment = psPayment.getGeneratedKeys();
            if (rsPayment.next()) {
                paymentId = rsPayment.getInt(1);
            }

            // 3. Mark coupon as SOLD
            String soldSql = "UPDATE coupons SET status = 'SOLD' WHERE coupon_id = ?";
            psCouponSold = conn.prepareStatement(soldSql);
            psCouponSold.setInt(1, couponId);
            psCouponSold.executeUpdate();

            // 4. Insert into purchases table
            String purchaseSql = "INSERT INTO purchases (buyer_id, coupon_id, payment_id, purchase_price, is_revealed, dispute_status) " +
                    "VALUES (?, ?, ?, ?, TRUE, 'NONE')";
            psPurchase = conn.prepareStatement(purchaseSql, Statement.RETURN_GENERATED_KEYS);
            psPurchase.setInt(1, buyerId);
            psPurchase.setInt(2, couponId);
            psPurchase.setInt(3, paymentId);
            psPurchase.setDouble(4, sellingPrice);
            psPurchase.executeUpdate();

            int purchaseId = -1;
            ResultSet rsPurchase = psPurchase.getGeneratedKeys();
            if (rsPurchase.next()) {
                purchaseId = rsPurchase.getInt(1);
            }

            // 5. Calculate platform commission and seller net earning
            double commissionRate = AppConfig.getCommissionRate(); // default 10%
            double commissionAmount = Math.round(sellingPrice * (commissionRate / 100.0) * 100.0) / 100.0;
            double netEarning = Math.round((sellingPrice - commissionAmount) * 100.0) / 100.0;

            String earningSql = "INSERT INTO seller_earnings (seller_id, coupon_id, purchase_id, gross_amount, commission_rate, commission_amount, net_earning, payout_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')";
            psEarning = conn.prepareStatement(earningSql);
            psEarning.setInt(1, sellerId);
            psEarning.setInt(2, couponId);
            psEarning.setInt(3, purchaseId);
            psEarning.setDouble(4, sellingPrice);
            psEarning.setDouble(5, commissionRate);
            psEarning.setDouble(6, commissionAmount);
            psEarning.setDouble(7, netEarning);
            psEarning.executeUpdate();

            // 6. Notifications for Seller and Buyer
            String notifSellerSql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, 'PURCHASE')";
            psNotifSeller = conn.prepareStatement(notifSellerSql);
            psNotifSeller.setInt(1, sellerId);
            psNotifSeller.setString(2, "Coupon Sold: " + brandName);
            psNotifSeller.setString(3, String.format("Your listing '%s' was bought for ₹%.2f. Net earning: ₹%.2f (after %.0f%% platform commission).",
                    couponTitle, sellingPrice, netEarning, commissionRate));
            psNotifSeller.executeUpdate();

            String notifBuyerSql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, 'SUCCESS')";
            psNotifBuyer = conn.prepareStatement(notifBuyerSql);
            psNotifBuyer.setInt(1, buyerId);
            psNotifBuyer.setString(2, "Purchase Confirmed: " + brandName);
            psNotifBuyer.setString(3, String.format("You unlocked code '%s' for '%s'. Enjoy your discount!", couponCode, couponTitle));
            psNotifBuyer.executeUpdate();

            conn.commit(); // Transaction successful!

            Purchase p = new Purchase();
            p.setPurchaseId(purchaseId);
            p.setBuyerId(buyerId);
            p.setCouponId(couponId);
            p.setCouponTitle(couponTitle);
            p.setBrandName(brandName);
            p.setCouponCode(couponCode);
            p.setPaymentId(paymentId);
            p.setTransactionId(txnId);
            p.setPaymentMethod(paymentMethod);
            p.setPurchasePrice(sellingPrice);
            p.setExpiryDate(expiryDate);
            p.setRevealed(true);
            p.setDisputeStatus("NONE");

            return new PurchaseResult(true, "Purchase completed successfully. Coupon code unlocked!", p);

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return new PurchaseResult(false, "Transaction failed: " + e.getMessage(), null);
        } finally {
            DBConnection.closeQuietly(psLock);
            DBConnection.closeQuietly(psPayment);
            DBConnection.closeQuietly(psCouponSold);
            DBConnection.closeQuietly(psPurchase);
            DBConnection.closeQuietly(psEarning);
            DBConnection.closeQuietly(psNotifSeller);
            DBConnection.closeQuietly(psNotifBuyer);
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<Purchase> findPurchasesByBuyer(int buyerId) throws SQLException {
        List<Purchase> list = new ArrayList<>();
        String sql = "SELECT p.*, cp.title as coupon_title, cp.brand_name, cp.coupon_code, cp.source_platform, " +
                "cp.usage_conditions, cp.expiry_date, c.name as category_name, pay.transaction_id, pay.payment_method, " +
                "(SELECT COUNT(*) FROM reviews r WHERE r.purchase_id = p.purchase_id) as review_count " +
                "FROM purchases p " +
                "JOIN coupons cp ON p.coupon_id = cp.coupon_id " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN payments pay ON p.payment_id = pay.payment_id " +
                "WHERE p.buyer_id = ? " +
                "ORDER BY p.purchase_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Purchase p = mapRow(rs);
                    p.setReviewed(rs.getInt("review_count") > 0);
                    list.add(p);
                }
            }
        }
        return list;
    }

    public Purchase findPurchaseById(int purchaseId, int buyerId) throws SQLException {
        String sql = "SELECT p.*, cp.title as coupon_title, cp.brand_name, cp.coupon_code, cp.source_platform, " +
                "cp.usage_conditions, cp.expiry_date, c.name as category_name, pay.transaction_id, pay.payment_method, " +
                "(SELECT COUNT(*) FROM reviews r WHERE r.purchase_id = p.purchase_id) as review_count " +
                "FROM purchases p " +
                "JOIN coupons cp ON p.coupon_id = cp.coupon_id " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN payments pay ON p.payment_id = pay.payment_id " +
                "WHERE p.purchase_id = ? AND (p.buyer_id = ? OR ? = 0)"; // 0 means admin

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            ps.setInt(2, buyerId);
            ps.setInt(3, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Purchase p = mapRow(rs);
                    p.setReviewed(rs.getInt("review_count") > 0);
                    return p;
                }
            }
        }
        return null;
    }

    public boolean isPurchasedByUser(int buyerId, int couponId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM purchases WHERE buyer_id = ? AND coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, couponId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Purchase mapRow(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setPurchaseId(rs.getInt("purchase_id"));
        p.setBuyerId(rs.getInt("buyer_id"));
        p.setCouponId(rs.getInt("coupon_id"));
        p.setCouponTitle(rs.getString("coupon_title"));
        p.setBrandName(rs.getString("brand_name"));
        p.setCouponCode(rs.getString("coupon_code"));
        p.setCategoryName(rs.getString("category_name"));
        p.setSourcePlatform(rs.getString("source_platform"));
        p.setUsageConditions(rs.getString("usage_conditions"));
        p.setExpiryDate(rs.getDate("expiry_date"));
        p.setPaymentId(rs.getInt("payment_id"));
        p.setTransactionId(rs.getString("transaction_id"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setPurchasePrice(rs.getDouble("purchase_price"));
        p.setPurchaseDate(rs.getTimestamp("purchase_date"));
        p.setRevealed(rs.getBoolean("is_revealed"));
        p.setDisputeStatus(rs.getString("dispute_status"));
        return p;
    }
}
