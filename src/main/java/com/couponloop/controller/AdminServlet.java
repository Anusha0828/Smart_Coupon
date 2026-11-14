package com.couponloop.controller;

import com.couponloop.dao.AdminDAO;
import com.couponloop.dao.CouponDAO;
import com.couponloop.dao.DisputeDAO;
import com.couponloop.dao.NotificationDAO;
import com.couponloop.dao.UserDAO;
import com.couponloop.model.Coupon;
import com.couponloop.model.CouponReport;
import com.couponloop.model.Dispute;
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
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    private final AdminDAO adminDAO = new AdminDAO();
    private final UserDAO userDAO = new UserDAO();
    private final DisputeDAO disputeDAO = new DisputeDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User admin = getAuthenticatedAdmin(req);
        if (admin == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required");
            return;
        }

        String path = req.getPathInfo();
        try {
            if ("/overview".equals(path)) {
                Map<String, Object> map = adminDAO.getPlatformOverview();
                JsonUtil.sendSuccess(resp, "Platform overview retrieved", map);
            } else if ("/users".equals(path)) {
                List<User> list = userDAO.findAllUsers();
                JsonUtil.sendSuccess(resp, "Users retrieved", list);
            } else if ("/coupons".equals(path)) {
                String status = req.getParameter("status");
                List<Coupon> list = adminDAO.getAllCoupons(status);
                JsonUtil.sendSuccess(resp, "Coupons retrieved", list);
            } else if ("/transactions".equals(path)) {
                List<Map<String, Object>> list = adminDAO.getAllTransactions();
                JsonUtil.sendSuccess(resp, "Transactions retrieved", list);
            } else if ("/disputes".equals(path)) {
                List<Dispute> disputes = disputeDAO.findAllDisputes();
                List<CouponReport> reports = disputeDAO.findAllReports();
                Map<String, Object> map = new HashMap<>();
                map.put("disputes", disputes);
                map.put("reports", reports);
                JsonUtil.sendSuccess(resp, "Disputes and reports retrieved", map);
            } else if ("/analytics".equals(path)) {
                List<Map<String, Object>> catDist = adminDAO.getCategoryDistribution();
                Map<String, Object> map = new HashMap<>();
                map.put("categoryDistribution", catDist);
                JsonUtil.sendSuccess(resp, "Analytics data retrieved", map);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown admin endpoint");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User admin = getAuthenticatedAdmin(req);
        if (admin == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required");
            return;
        }

        String path = req.getPathInfo();
        JsonObject json = JsonUtil.parseJsonObject(req);

        try {
            if ("/approve-coupon".equals(path)) {
                int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;
                if (couponId <= 0) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid couponId required.");
                    return;
                }
                Coupon coupon = couponDAO.findByIdRaw(couponId);
                if (coupon == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Coupon not found.");
                    return;
                }

                boolean approved = adminDAO.approveCoupon(couponId, admin.getUserId());
                if (approved) {
                    adminDAO.recordAction(admin.getUserId(), "APPROVE_COUPON", "COUPON", couponId, "Coupon approved and published to marketplace");
                    notificationDAO.create(coupon.getSellerId(), "Coupon Approved!",
                            "Your coupon '" + coupon.getTitle() + "' has been approved by admin and is now available in the marketplace.", "SUCCESS");
                    JsonUtil.sendSuccess(resp, "Coupon approved successfully. It is now available in the marketplace.");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to approve coupon.");
                }

            } else if ("/reject-coupon".equals(path)) {
                int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;
                String rejectionReason = json.has("rejectionReason") ? json.get("rejectionReason").getAsString().trim() : "";
                if (couponId <= 0 || rejectionReason.isEmpty()) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Coupon ID and rejection reason are required.");
                    return;
                }
                Coupon coupon = couponDAO.findByIdRaw(couponId);
                if (coupon == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Coupon not found.");
                    return;
                }

                boolean rejected = adminDAO.rejectCoupon(couponId, admin.getUserId(), rejectionReason);
                if (rejected) {
                    adminDAO.recordAction(admin.getUserId(), "REJECT_COUPON", "COUPON", couponId, "Reason: " + rejectionReason);
                    notificationDAO.create(coupon.getSellerId(), "Coupon Rejected",
                            "Your coupon '" + coupon.getTitle() + "' was rejected by admin. Reason: " + rejectionReason, "WARNING");
                    JsonUtil.sendSuccess(resp, "Coupon rejected successfully. The seller has been notified.");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to reject coupon.");
                }

            } else if ("/toggle-user-status".equals(path)) {
                int targetUserId = json.has("userId") ? json.get("userId").getAsInt() : 0;
                String newStatus = json.has("status") ? json.get("status").getAsString() : "ACTIVE";

                if (targetUserId == admin.getUserId()) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "You cannot suspend your own admin account.");
                    return;
                }

                boolean updated = userDAO.updateStatus(targetUserId, newStatus);
                if (updated) {
                    adminDAO.recordAction(admin.getUserId(), "TOGGLE_USER_STATUS", "USER", targetUserId, "Status set to " + newStatus);
                    JsonUtil.sendSuccess(resp, "User status updated to " + newStatus);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update user status");
                }
            } else if ("/update-coupon-status".equals(path)) {
                int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;
                String status = json.has("status") ? json.get("status").getAsString() : "ACTIVE";

                boolean updated = adminDAO.updateCouponStatus(couponId, status);
                if (updated) {
                    adminDAO.recordAction(admin.getUserId(), "UPDATE_COUPON_STATUS", "COUPON", couponId, "Status updated to " + status);
                    JsonUtil.sendSuccess(resp, "Coupon status updated to " + status);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update coupon status");
                }
            } else if ("/resolve-dispute".equals(path)) {
                int disputeId = json.has("disputeId") ? json.get("disputeId").getAsInt() : 0;
                String resolution = json.has("resolution") ? json.get("resolution").getAsString() : "RESOLVED_REFUND";
                String notes = json.has("notes") ? json.get("notes").getAsString() : "Reviewed by admin.";

                boolean resolved = disputeDAO.resolveDispute(disputeId, resolution, notes, admin.getUserId());
                if (resolved) {
                    adminDAO.recordAction(admin.getUserId(), "RESOLVE_DISPUTE", "DISPUTE", disputeId, "Resolution: " + resolution + " | Notes: " + notes);
                    JsonUtil.sendSuccess(resp, "Dispute case resolved successfully.");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to resolve dispute.");
                }
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown admin POST action");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private User getAuthenticatedAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User u = (User) session.getAttribute("user");
            if (u.isAdmin()) {
                return u;
            }
        }
        return null;
    }
}
