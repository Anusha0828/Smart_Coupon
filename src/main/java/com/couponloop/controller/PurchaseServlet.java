package com.couponloop.controller;

import com.couponloop.dao.CouponDAO;
import com.couponloop.dao.PurchaseDAO;
import com.couponloop.model.Coupon;
import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/purchase/*")
public class PurchaseServlet extends HttpServlet {

    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in to proceed with checkout.");
            return;
        }

        String path = req.getPathInfo();
        if ("/preview".equals(path)) {
            handlePreview(req, resp, user);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown purchase endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in to complete payment.");
            return;
        }

        String path = req.getPathInfo();
        if ("/pay".equals(path)) {
            handlePayment(req, resp, user);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown purchase POST endpoint");
        }
    }

    private void handlePreview(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        String couponIdStr = req.getParameter("couponId");
        if (couponIdStr == null || couponIdStr.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing coupon ID");
            return;
        }

        try {
            int couponId = Integer.parseInt(couponIdStr.trim());
            Coupon coupon = couponDAO.findById(couponId, user.getUserId());
            if (coupon == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Coupon not found");
                return;
            }

            if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus()) && !"APPROVED".equalsIgnoreCase(coupon.getStatus())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "This coupon is no longer available.");
                return;
            }

            if (coupon.getSellerId() == user.getUserId()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "You cannot purchase your own coupon listing.");
                return;
            }

            Map<String, Object> preview = new HashMap<>();
            preview.put("couponId", coupon.getCouponId());
            preview.put("title", coupon.getTitle());
            preview.put("brandName", coupon.getBrandName());
            preview.put("sellingPrice", coupon.getSellingPrice());
            preview.put("originalValue", coupon.getOriginalValue());
            preview.put("savingsAmount", coupon.getSavingsAmount());
            preview.put("expiryDate", coupon.getExpiryDate());
            preview.put("usageConditions", coupon.getUsageConditions());
            preview.put("sellerName", coupon.getSellerName());
            preview.put("platformFee", 0.00); // Platform fee is paid via seller commission
            preview.put("totalPayable", coupon.getSellingPrice());

            JsonUtil.sendSuccess(resp, "Checkout preview ready", preview);

        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handlePayment(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;
        String paymentMethod = json.has("paymentMethod") ? json.get("paymentMethod").getAsString() : "UPI_DEMO";
        String simulateResult = json.has("simulateResult") ? json.get("simulateResult").getAsString() : "SUCCESS";

        if (couponId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid coupon ID is required");
            return;
        }

        // Test payment failure simulation
        if ("FAILURE".equalsIgnoreCase(simulateResult)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED,
                    "Sandbox payment simulation: Transaction declined by issuing bank/UPI provider. No funds deducted.");
            return;
        }

        // Execute atomic purchase transaction
        PurchaseDAO.PurchaseResult result = purchaseDAO.executePurchase(user.getUserId(), couponId, paymentMethod, null);

        if (result.success) {
            JsonUtil.sendSuccess(resp, result.message, result.purchase);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, result.message);
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
