package com.couponloop.dao;

import com.couponloop.config.DBConnection;
import com.couponloop.model.Coupon;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {

    public List<Coupon> searchCoupons(String keyword, Integer categoryId, String brand,
                                      Double minPrice, Double maxPrice, Integer maxDaysToExpiry,
                                      String sortBy, int limit, int offset, Integer currentUserId) throws SQLException {
        List<Coupon> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, ")
                .append("COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.seller_id = cp.seller_id), 5.0) as seller_rating ");

        if (currentUserId != null && currentUserId > 0) {
            sql.append(", (SELECT COUNT(*) FROM wishlist w WHERE w.user_id = ? AND w.coupon_id = cp.coupon_id) as in_wishlist ");
        } else {
            sql.append(", 0 as in_wishlist ");
        }

        sql.append("FROM coupons cp ")
                .append("JOIN coupon_categories c ON cp.category_id = c.category_id ")
                .append("JOIN users u ON cp.seller_id = u.user_id ")
                .append("WHERE cp.status IN ('APPROVED', 'ACTIVE') AND cp.expiry_date >= CURRENT_DATE ");

        List<Object> params = new ArrayList<>();
        if (currentUserId != null && currentUserId > 0) {
            params.add(currentUserId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(cp.title) LIKE ? OR LOWER(cp.brand_name) LIKE ? OR LOWER(cp.description) LIKE ? OR LOWER(c.name) LIKE ?) ");
            String k = "%" + keyword.trim().toLowerCase() + "%";
            params.add(k);
            params.add(k);
            params.add(k);
            params.add(k);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND cp.category_id = ? ");
            params.add(categoryId);
        }

        if (brand != null && !brand.trim().isEmpty()) {
            sql.append("AND LOWER(cp.brand_name) = LOWER(?) ");
            params.add(brand.trim());
        }

        if (minPrice != null && minPrice >= 0) {
            sql.append("AND cp.selling_price >= ? ");
            params.add(minPrice);
        }

        if (maxPrice != null && maxPrice > 0) {
            sql.append("AND cp.selling_price <= ? ");
            params.add(maxPrice);
        }

        if (maxDaysToExpiry != null && maxDaysToExpiry > 0) {
            sql.append("AND cp.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) ");
            params.add(maxDaysToExpiry);
        }

        // Sorting
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY cp.selling_price ASC ");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY cp.selling_price DESC ");
        } else if ("discount_desc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY (cp.original_value - cp.selling_price) DESC ");
        } else if ("discount_pct_desc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY ((cp.original_value - cp.selling_price) / cp.original_value) DESC ");
        } else if ("expiring_soon".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY cp.expiry_date ASC ");
        } else {
            sql.append("ORDER BY cp.created_at DESC "); // Default newest
        }

        if (limit <= 0) limit = 20;
        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(Math.max(0, offset));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Coupon coupon = mapRow(rs);
                    // Mask coupon code in public search results
                    coupon.setCouponCode("••••••••");
                    coupon.setInWishlist(rs.getInt("in_wishlist") > 0);
                    list.add(coupon);
                }
            }
        }
        return list;
    }

    public Coupon findById(int couponId, Integer requestingUserId) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, ")
                .append("COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.seller_id = cp.seller_id), 5.0) as seller_rating ");

        if (requestingUserId != null && requestingUserId > 0) {
            sql.append(", (SELECT COUNT(*) FROM wishlist w WHERE w.user_id = ? AND w.coupon_id = cp.coupon_id) as in_wishlist ");
            sql.append(", (SELECT COUNT(*) FROM purchases p WHERE p.buyer_id = ? AND p.coupon_id = cp.coupon_id) as is_purchased ");
        } else {
            sql.append(", 0 as in_wishlist, 0 as is_purchased ");
        }

        sql.append("FROM coupons cp ")
                .append("JOIN coupon_categories c ON cp.category_id = c.category_id ")
                .append("JOIN users u ON cp.seller_id = u.user_id ")
                .append("WHERE cp.coupon_id = ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int pIndex = 1;
            if (requestingUserId != null && requestingUserId > 0) {
                ps.setInt(pIndex++, requestingUserId);
                ps.setInt(pIndex++, requestingUserId);
            }
            ps.setInt(pIndex, couponId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Coupon c = mapRow(rs);
                    boolean isPurchased = rs.getInt("is_purchased") > 0;
                    boolean isOwner = requestingUserId != null && requestingUserId == c.getSellerId();

                    // Only approved/active coupons or coupons viewed by owner can be viewed
                    if (!isOwner && !"APPROVED".equalsIgnoreCase(c.getStatus()) && !"ACTIVE".equalsIgnoreCase(c.getStatus())) {
                        return null;
                    }

                    c.setInWishlist(rs.getInt("in_wishlist") > 0);

                    // Mask coupon code unless user is the seller or verified buyer
                    if (!isPurchased && !isOwner) {
                        c.setCouponCode("••••••••");
                    }
                    return c;
                }
            }
        }
        return null;
    }

    public Coupon findByIdRaw(int couponId) throws SQLException {
        String sql = "SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, 5.0 as seller_rating " +
                "FROM coupons cp " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN users u ON cp.seller_id = u.user_id " +
                "WHERE cp.coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean isDuplicateCouponCode(String couponCode) throws SQLException {
        if (couponCode == null || couponCode.trim().isEmpty()) return false;
        String sql = "SELECT coupon_id FROM coupons WHERE UPPER(TRIM(coupon_code)) = UPPER(TRIM(?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, couponCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isDuplicateCouponForSeller(int sellerId, String brandName, String title) throws SQLException {
        if (brandName == null || title == null) return false;
        String sql = "SELECT coupon_id FROM coupons WHERE seller_id = ? AND UPPER(TRIM(brand_name)) = UPPER(TRIM(?)) " +
                     "AND UPPER(TRIM(title)) = UPPER(TRIM(?)) AND status IN ('PENDING', 'APPROVED', 'ACTIVE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            ps.setString(2, brandName.trim());
            ps.setString(3, title.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int create(Coupon coupon) throws SQLException {
        String sql = "INSERT INTO coupons (seller_id, category_id, title, brand_name, source_platform, " +
                "coupon_code, description, original_value, selling_price, min_purchase_amount, " +
                "expiry_date, usage_conditions, image_url, is_transferable_confirmed, terms_confirmed, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, coupon.getSellerId());
            ps.setInt(2, coupon.getCategoryId());
            ps.setString(3, coupon.getTitle());
            ps.setString(4, coupon.getBrandName());
            ps.setString(5, coupon.getSourcePlatform());
            ps.setString(6, coupon.getCouponCode());
            ps.setString(7, coupon.getDescription());
            ps.setDouble(8, coupon.getOriginalValue());
            ps.setDouble(9, coupon.getSellingPrice());
            ps.setDouble(10, coupon.getMinPurchaseAmount());
            ps.setDate(11, coupon.getExpiryDate());
            ps.setString(12, coupon.getUsageConditions());
            ps.setString(13, coupon.getImageUrl());
            ps.setBoolean(14, coupon.isTransferableConfirmed());
            ps.setBoolean(15, coupon.isTermsConfirmed());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Coupon coupon) throws SQLException {
        String sql = "UPDATE coupons SET category_id = ?, title = ?, brand_name = ?, source_platform = ?, " +
                "description = ?, original_value = ?, selling_price = ?, min_purchase_amount = ?, " +
                "expiry_date = ?, usage_conditions = ?, image_url = ?, status = 'PENDING' " +
                "WHERE coupon_id = ? AND seller_id = ? AND status IN ('PENDING', 'APPROVED', 'ACTIVE', 'REJECTED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coupon.getCategoryId());
            ps.setString(2, coupon.getTitle());
            ps.setString(3, coupon.getBrandName());
            ps.setString(4, coupon.getSourcePlatform());
            ps.setString(5, coupon.getDescription());
            ps.setDouble(6, coupon.getOriginalValue());
            ps.setDouble(7, coupon.getSellingPrice());
            ps.setDouble(8, coupon.getMinPurchaseAmount());
            ps.setDate(9, coupon.getExpiryDate());
            ps.setString(10, coupon.getUsageConditions());
            ps.setString(11, coupon.getImageUrl());
            ps.setInt(12, coupon.getCouponId());
            ps.setInt(13, coupon.getSellerId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int couponId, int sellerId) throws SQLException {
        String sql = "DELETE FROM coupons WHERE coupon_id = ? AND seller_id = ? AND status IN ('PENDING', 'APPROVED', 'ACTIVE', 'REJECTED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, sellerId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Coupon> findBySellerId(int sellerId) throws SQLException {
        List<Coupon> list = new ArrayList<>();
        String sql = "SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, 5.0 as seller_rating " +
                "FROM coupons cp " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN users u ON cp.seller_id = u.user_id " +
                "WHERE cp.seller_id = ? " +
                "ORDER BY cp.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Coupon> findExpiringSoonForSeller(int sellerId, int days) throws SQLException {
        List<Coupon> list = new ArrayList<>();
        String sql = "SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, 5.0 as seller_rating " +
                "FROM coupons cp " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN users u ON cp.seller_id = u.user_id " +
                "WHERE cp.seller_id = ? AND cp.status IN ('APPROVED', 'ACTIVE') " +
                "AND cp.expiry_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) " +
                "ORDER BY cp.expiry_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Coupon> getRecommendations(Integer currentUserId, int limit) throws SQLException {
        List<Coupon> list = new ArrayList<>();
        String sql = "SELECT cp.*, c.name as category_name, c.slug as category_slug, u.full_name as seller_name, u.email as seller_email, 5.0 as seller_rating " +
                "FROM coupons cp " +
                "JOIN coupon_categories c ON cp.category_id = c.category_id " +
                "JOIN users u ON cp.seller_id = u.user_id " +
                "WHERE cp.status IN ('APPROVED', 'ACTIVE') AND cp.expiry_date >= CURRENT_DATE " +
                "ORDER BY cp.views_count DESC, cp.created_at DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 6);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Coupon coupon = mapRow(rs);
                    coupon.setCouponCode("••••••••");
                    list.add(coupon);
                }
            }
        }
        return list;
    }

    public List<String> getDistinctBrands() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT brand_name FROM coupons WHERE status IN ('APPROVED', 'ACTIVE') AND expiry_date >= CURRENT_DATE ORDER BY brand_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("brand_name"));
            }
        }
        return list;
    }

    public void incrementViews(int couponId) {
        String sql = "UPDATE coupons SET views_count = views_count + 1 WHERE coupon_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public int markExpiredCoupons() {
        String sql = "UPDATE coupons SET status = 'EXPIRED' WHERE status IN ('APPROVED', 'ACTIVE', 'PENDING') AND expiry_date < CURRENT_DATE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error auto-expiring coupons: " + e.getMessage());
            return 0;
        }
    }

    private Coupon mapRow(ResultSet rs) throws SQLException {
        Coupon c = new Coupon();
        c.setCouponId(rs.getInt("coupon_id"));
        c.setSellerId(rs.getInt("seller_id"));
        c.setCategoryId(rs.getInt("category_id"));
        c.setCategoryName(rs.getString("category_name"));
        c.setCategorySlug(rs.getString("category_slug"));
        c.setSellerName(rs.getString("seller_name"));
        try {
            c.setSellerEmail(rs.getString("seller_email"));
        } catch (SQLException ignored) {}
        c.setSellerRating(rs.getDouble("seller_rating"));
        c.setTitle(rs.getString("title"));
        c.setBrandName(rs.getString("brand_name"));
        c.setSourcePlatform(rs.getString("source_platform"));
        c.setCouponCode(rs.getString("coupon_code"));
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
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }
}
