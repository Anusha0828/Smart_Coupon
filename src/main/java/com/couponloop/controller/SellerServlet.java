package com.couponloop.controller;

import com.couponloop.dao.CouponDAO;
import com.couponloop.dao.NotificationDAO;
import com.couponloop.dao.SellerEarningDAO;
import com.couponloop.model.Coupon;
import com.couponloop.model.SellerEarning;
import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
import com.couponloop.util.ValidationUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/seller/*")
public class SellerServlet extends HttpServlet {

    private final CouponDAO couponDAO = new CouponDAO();
    private final SellerEarningDAO sellerEarningDAO = new SellerEarningDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String path = req.getPathInfo();
        try {
            if ("/dashboard".equals(path)) {
                handleDashboard(req, resp, user);
            } else if ("/my-coupons".equals(path)) {
                handleMyCoupons(req, resp, user);
            } else if ("/earnings".equals(path)) {
                handleEarnings(req, resp, user);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown seller endpoint");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String path = req.getPathInfo();
        try {
            if ("/upload".equals(path)) {
                handleUpload(req, resp, user);
            } else if ("/update".equals(path)) {
                handleUpdate(req, resp, user);
            } else if ("/delete".equals(path)) {
                handleDelete(req, resp, user);
            } else if ("/request-payout".equals(path)) {
                handleRequestPayout(req, resp, user);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown seller POST endpoint");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleDashboard(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        Map<String, Object> stats = sellerEarningDAO.getSellerDashboardStats(user.getUserId());
        List<Coupon> expiringSoon = couponDAO.findExpiringSoonForSeller(user.getUserId(), 7);

        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("expiringSoon", expiringSoon);
        result.put("userUpiId", user.getUpiId());

        JsonUtil.sendSuccess(resp, "Seller dashboard loaded", result);
    }

    private void handleMyCoupons(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        List<Coupon> list = couponDAO.findBySellerId(user.getUserId());
        JsonUtil.sendSuccess(resp, "Seller listings retrieved", list);
    }

    private void handleEarnings(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        List<SellerEarning> list = sellerEarningDAO.findEarningsBySeller(user.getUserId());
        JsonUtil.sendSuccess(resp, "Earnings history retrieved", list);
    }

    private void handleUpload(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);

        String title = json.has("title") ? json.get("title").getAsString() : "";
        String brandName = json.has("brandName") ? json.get("brandName").getAsString() : "";
        int categoryId = json.has("categoryId") ? json.get("categoryId").getAsInt() : 0;
        String sourcePlatform = json.has("sourcePlatform") ? json.get("sourcePlatform").getAsString() : "";
        String couponCode = json.has("couponCode") ? json.get("couponCode").getAsString() : "";
        String description = json.has("description") ? json.get("description").getAsString() : "";
        double originalValue = json.has("originalValue") ? json.get("originalValue").getAsDouble() : 0.0;
        double sellingPrice = json.has("sellingPrice") ? json.get("sellingPrice").getAsDouble() : 0.0;
        double minPurchaseAmount = json.has("minPurchaseAmount") ? json.get("minPurchaseAmount").getAsDouble() : 0.0;
        String expiryDateStr = json.has("expiryDate") ? json.get("expiryDate").getAsString() : null;
        String usageConditions = json.has("usageConditions") ? json.get("usageConditions").getAsString() : "";
        String imageUrl = json.has("imageUrl") ? json.get("imageUrl").getAsString() : "";
        boolean transferableConfirmed = json.has("transferableConfirmed") && json.get("transferableConfirmed").getAsBoolean();
        boolean termsConfirmed = json.has("termsConfirmed") && json.get("termsConfirmed").getAsBoolean();

        // Validations
        if (!ValidationUtil.isNotEmpty(title) || !ValidationUtil.isNotEmpty(brandName) || !ValidationUtil.isNotEmpty(couponCode)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title, Brand, and Coupon Code are required.");
            return;
        }

        if (categoryId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please select a valid coupon category.");
            return;
        }

        if (!ValidationUtil.isValidCouponPrice(originalValue, sellingPrice)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Selling price must be greater than zero and cannot exceed original coupon value.");
            return;
        }

        if (expiryDateStr == null || expiryDateStr.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Expiry date is required.");
            return;
        }

        LocalDate expDate;
        try {
            expDate = LocalDate.parse(expiryDateStr.trim());
            if (!ValidationUtil.isFutureDate(expDate)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Coupon expiry date must be in the future (minimum tomorrow).");
                return;
            }
        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid expiry date format. Use YYYY-MM-DD.");
            return;
        }

        if (!transferableConfirmed) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "You must confirm that this voucher is transferable under merchant issuer terms.");
            return;
        }

        if (!termsConfirmed) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "You must agree to CouponLoop terms and platform rules.");
            return;
        }

        // Duplicate coupon checks
        if (couponDAO.isDuplicateCouponCode(couponCode.trim())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "A coupon with this code has already been submitted to the platform. Duplicate coupons are not permitted.");
            return;
        }

        if (couponDAO.isDuplicateCouponForSeller(user.getUserId(), brandName.trim(), title.trim())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "You have already submitted this coupon listing. Duplicate submissions are not allowed.");
            return;
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=600&auto=format&fit=crop&q=80";
        }

        Coupon coupon = new Coupon();
        coupon.setSellerId(user.getUserId());
        coupon.setCategoryId(categoryId);
        coupon.setTitle(ValidationUtil.sanitize(title));
        coupon.setBrandName(ValidationUtil.sanitize(brandName));
        coupon.setSourcePlatform(ValidationUtil.sanitize(sourcePlatform));
        coupon.setCouponCode(couponCode.trim());
        coupon.setDescription(ValidationUtil.sanitize(description));
        coupon.setOriginalValue(originalValue);
        coupon.setSellingPrice(sellingPrice);
        coupon.setMinPurchaseAmount(minPurchaseAmount);
        coupon.setExpiryDate(Date.valueOf(expDate));
        coupon.setUsageConditions(ValidationUtil.sanitize(usageConditions));
        coupon.setImageUrl(imageUrl.trim());
        coupon.setTransferableConfirmed(true);
        coupon.setTermsConfirmed(true);
        coupon.setStatus("PENDING");

        int newId = couponDAO.create(coupon);
        if (newId > 0) {
            notificationDAO.create(user.getUserId(), "Coupon Submitted for Approval",
                    "Your coupon '" + title + "' has been submitted successfully. It is waiting for admin approval.", "INFO");

            Map<String, Object> data = new HashMap<>();
            data.put("couponId", newId);
            data.put("status", "PENDING");
            JsonUtil.sendSuccess(resp, "Your coupon has been submitted successfully. It is waiting for admin approval.", data);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create coupon listing.");
        }
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;
        int categoryId = json.has("categoryId") ? json.get("categoryId").getAsInt() : 0;
        String title = json.has("title") ? json.get("title").getAsString() : "";
        String brandName = json.has("brandName") ? json.get("brandName").getAsString() : "";
        String sourcePlatform = json.has("sourcePlatform") ? json.get("sourcePlatform").getAsString() : "";
        String description = json.has("description") ? json.get("description").getAsString() : "";
        double originalValue = json.has("originalValue") ? json.get("originalValue").getAsDouble() : 0.0;
        double sellingPrice = json.has("sellingPrice") ? json.get("sellingPrice").getAsDouble() : 0.0;
        double minPurchaseAmount = json.has("minPurchaseAmount") ? json.get("minPurchaseAmount").getAsDouble() : 0.0;
        String expiryDateStr = json.has("expiryDate") ? json.get("expiryDate").getAsString() : null;
        String usageConditions = json.has("usageConditions") ? json.get("usageConditions").getAsString() : "";
        String imageUrl = json.has("imageUrl") ? json.get("imageUrl").getAsString() : "";

        if (couponId <= 0 || !ValidationUtil.isValidCouponPrice(originalValue, sellingPrice)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid coupon update parameters.");
            return;
        }

        LocalDate expDate;
        try {
            expDate = LocalDate.parse(expiryDateStr.trim());
            if (!ValidationUtil.isFutureDate(expDate)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Expiry date must be in the future.");
                return;
            }
        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid expiry date format.");
            return;
        }

        Coupon coupon = new Coupon();
        coupon.setCouponId(couponId);
        coupon.setSellerId(user.getUserId());
        coupon.setCategoryId(categoryId);
        coupon.setTitle(ValidationUtil.sanitize(title));
        coupon.setBrandName(ValidationUtil.sanitize(brandName));
        coupon.setSourcePlatform(ValidationUtil.sanitize(sourcePlatform));
        coupon.setDescription(ValidationUtil.sanitize(description));
        coupon.setOriginalValue(originalValue);
        coupon.setSellingPrice(sellingPrice);
        coupon.setMinPurchaseAmount(minPurchaseAmount);
        coupon.setExpiryDate(Date.valueOf(expDate));
        coupon.setUsageConditions(ValidationUtil.sanitize(usageConditions));
        coupon.setImageUrl(imageUrl);

        boolean updated = couponDAO.update(coupon);
        if (updated) {
            JsonUtil.sendSuccess(resp, "Listing updated successfully.");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot update this coupon. It may already be sold or not belong to you.");
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;

        if (couponId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid coupon ID");
            return;
        }

        boolean deleted = couponDAO.delete(couponId, user.getUserId());
        if (deleted) {
            JsonUtil.sendSuccess(resp, "Coupon listing removed.");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unable to delete listing. Sold coupons cannot be deleted.");
        }
    }

    private void handleRequestPayout(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String upiId = json.has("upiId") ? json.get("upiId").getAsString() : user.getUpiId();

        if (upiId == null || !upiId.contains("@")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "A valid UPI ID is required for payout clearance.");
            return;
        }

        boolean success = sellerEarningDAO.requestPayout(user.getUserId(), upiId);
        if (success) {
            JsonUtil.sendSuccess(resp, "Payout request submitted safely. Pending earnings have been cleared to " + upiId + ".");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process payout.");
        }
    }

    private User getAuthenticatedUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
}
