package com.couponloop.controller;

import com.couponloop.dao.CouponDAO;
import com.couponloop.dao.DisputeDAO;
import com.couponloop.dao.PurchaseDAO;
import com.couponloop.model.Coupon;
import com.couponloop.model.Dispute;
import com.couponloop.model.Purchase;
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
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/disputes/*")
public class DisputeServlet extends HttpServlet {

    private final DisputeDAO disputeDAO = new DisputeDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        try {
            List<Dispute> list = disputeDAO.findByBuyer(user.getUserId());
            JsonUtil.sendSuccess(resp, "Disputes retrieved", list);
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
        if ("/raise".equals(path)) {
            handleRaiseDispute(req, resp, user);
        } else if ("/report-coupon".equals(path)) {
            handleReportCoupon(req, resp, user);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown dispute endpoint");
        }
    }

    private void handleRaiseDispute(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        int purchaseId = json.has("purchaseId") ? json.get("purchaseId").getAsInt() : 0;
        String reason = json.has("reason") ? json.get("reason").getAsString() : "";
        String details = json.has("details") ? json.get("details").getAsString() : "";
        String evidenceUrl = json.has("evidenceUrl") ? json.get("evidenceUrl").getAsString() : "";

        if (purchaseId <= 0 || !ValidationUtil.isNotEmpty(reason) || !ValidationUtil.isNotEmpty(details)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Purchase ID, dispute reason, and details are required.");
            return;
        }

        try {
            Purchase purchase = purchaseDAO.findPurchaseById(purchaseId, user.getUserId());
            if (purchase == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Unauthorized: You can only raise disputes for your own purchases.");
                return;
            }

            Coupon coupon = couponDAO.findByIdRaw(purchase.getCouponId());
            if (coupon == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Coupon record missing.");
                return;
            }

            Dispute d = new Dispute();
            d.setPurchaseId(purchaseId);
            d.setCouponId(purchase.getCouponId());
            d.setBuyerId(user.getUserId());
            d.setSellerId(coupon.getSellerId());
            d.setReason(ValidationUtil.sanitize(reason));
            d.setDetails(ValidationUtil.sanitize(details));
            d.setEvidenceUrl(evidenceUrl != null ? evidenceUrl.trim() : null);

            boolean created = disputeDAO.createDispute(d);
            if (created) {
                JsonUtil.sendSuccess(resp, "Dispute case opened. Our administrative team will review your report within 24 hours.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to record dispute.");
            }

        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleReportCoupon(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;
        String reason = json.has("reason") ? json.get("reason").getAsString() : "";
        String details = json.has("details") ? json.get("details").getAsString() : "";

        if (couponId <= 0 || !ValidationUtil.isNotEmpty(reason)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Coupon ID and report reason required.");
            return;
        }

        try {
            boolean success = disputeDAO.createReport(couponId, user.getUserId(), ValidationUtil.sanitize(reason), ValidationUtil.sanitize(details));
            if (success) {
                JsonUtil.sendSuccess(resp, "Thank you. Your report has been submitted for administrative review.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit report.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
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
