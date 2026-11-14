package com.couponloop.controller;

import com.couponloop.dao.NotificationDAO;
import com.couponloop.dao.UserDAO;
import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
import com.couponloop.util.PasswordUtil;
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
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/user/*")
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        try {
            User user = userDAO.findById(currentUser.getUserId());
            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getUserId());
            map.put("fullName", user.getFullName());
            map.put("email", user.getEmail());
            map.put("mobile", user.getMobile());
            map.put("role", user.getRole());
            map.put("status", user.getStatus());
            map.put("upiId", user.getUpiId());
            map.put("createdAt", user.getCreatedAt());

            JsonUtil.sendSuccess(resp, "Profile retrieved successfully", map);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String path = req.getPathInfo();
        if ("/update-profile".equals(path)) {
            handleUpdateProfile(req, resp, currentUser);
        } else if ("/change-password".equals(path)) {
            handleChangePassword(req, resp, currentUser);
        } else if ("/update-upi".equals(path)) {
            handleUpdateUpi(req, resp, currentUser);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown user endpoint");
        }
    }

    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String fullName = json.has("fullName") ? json.get("fullName").getAsString() : "";
        String mobile = json.has("mobile") ? json.get("mobile").getAsString() : "";

        if (!ValidationUtil.isNotEmpty(fullName) || fullName.trim().length() < 2) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Full name must be at least 2 characters.");
            return;
        }

        if (!ValidationUtil.isValidMobile(mobile)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid 10-digit mobile number.");
            return;
        }

        try {
            boolean updated = userDAO.updateProfile(user.getUserId(), ValidationUtil.sanitize(fullName), mobile.trim());
            if (updated) {
                user.setFullName(fullName);
                user.setMobile(mobile);
                req.getSession().setAttribute("user", user);
                JsonUtil.sendSuccess(resp, "Profile updated successfully.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to update profile.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleUpdateUpi(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String upiId = json.has("upiId") ? json.get("upiId").getAsString() : "";

        if (!ValidationUtil.isNotEmpty(upiId) || !upiId.contains("@")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid UPI ID (e.g. name@bank).");
            return;
        }

        try {
            boolean updated = userDAO.updateUpiId(user.getUserId(), upiId.trim());
            if (updated) {
                user.setUpiId(upiId.trim());
                req.getSession().setAttribute("user", user);
                JsonUtil.sendSuccess(resp, "Payout UPI ID updated successfully.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to update UPI ID.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String currentPassword = json.has("currentPassword") ? json.get("currentPassword").getAsString() : "";
        String newPassword = json.has("newPassword") ? json.get("newPassword").getAsString() : "";
        String confirmPassword = json.has("confirmPassword") ? json.get("confirmPassword").getAsString() : "";

        try {
            User dbUser = userDAO.findById(user.getUserId());
            if (!PasswordUtil.verifyPassword(currentPassword, dbUser.getSalt(), dbUser.getPasswordHash())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Current password is incorrect.");
                return;
            }

            if (!ValidationUtil.isValidPassword(newPassword) || !newPassword.equals(confirmPassword)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "New password must be at least 6 characters and match confirmation.");
                return;
            }

            String newSalt = PasswordUtil.generateSalt();
            String newHash = PasswordUtil.hashPassword(newPassword, newSalt);

            boolean updated = userDAO.updatePassword(user.getUserId(), newHash, newSalt);
            if (updated) {
                notificationDAO.create(user.getUserId(), "Password Changed",
                        "Your account password was updated successfully.", "INFO");
                JsonUtil.sendSuccess(resp, "Password changed successfully.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to change password.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
}
