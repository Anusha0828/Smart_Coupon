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

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/current-user".equals(pathInfo)) {
            handleCurrentUser(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing auth action");
            return;
        }

        switch (pathInfo) {
            case "/register":
                handleRegister(req, resp);
                break;
            case "/login":
                handleLogin(req, resp);
                break;
            case "/logout":
                handleLogout(req, resp);
                break;
            case "/forgot-password":
                handleForgotPassword(req, resp);
                break;
            case "/reset-password":
                handleResetPassword(req, resp);
                break;
            default:
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown auth action");
                break;
        }
    }

    private void handleCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            // Refresh from DB to reflect any status or profile changes
            try {
                User fresh = userDAO.findById(user.getUserId());
                if (fresh != null) {
                    session.setAttribute("user", fresh);
                    user = fresh;
                }
            } catch (SQLException ignored) {
            }

            Map<String, Object> map = new HashMap<>();
            map.put("loggedIn", true);
            map.put("userId", user.getUserId());
            map.put("fullName", user.getFullName());
            map.put("email", user.getEmail());
            map.put("mobile", user.getMobile());
            map.put("role", user.getRole());
            map.put("status", user.getStatus());
            map.put("upiId", user.getUpiId());
            map.put("isAdmin", user.isAdmin());

            JsonUtil.sendSuccess(resp, "Current user session active", map);
        } else {
            Map<String, Object> map = new HashMap<>();
            map.put("loggedIn", false);
            JsonUtil.sendSuccess(resp, "No active user session", map);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String fullName = json.has("fullName") ? json.get("fullName").getAsString() : "";
        String email = json.has("email") ? json.get("email").getAsString() : "";
        String mobile = json.has("mobile") ? json.get("mobile").getAsString() : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";
        String confirmPassword = json.has("confirmPassword") ? json.get("confirmPassword").getAsString() : "";
        String role = json.has("role") ? json.get("role").getAsString() : "BUYER";

        // Input validation
        if (!ValidationUtil.isNotEmpty(fullName) || fullName.trim().length() < 2) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Full name must be at least 2 characters.");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid email address.");
            return;
        }

        if (!ValidationUtil.isValidMobile(mobile)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid 10-digit mobile number.");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters long.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Passwords do not match.");
            return;
        }

        try {
            // Check if user already exists
            User existing = userDAO.findByEmail(email);
            if (existing != null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "An account with this email already exists. Please login.");
                return;
            }

            String salt = PasswordUtil.generateSalt();
            String passwordHash = PasswordUtil.hashPassword(password, salt);

            User newUser = new User();
            newUser.setFullName(ValidationUtil.sanitize(fullName));
            newUser.setEmail(email.trim().toLowerCase());
            newUser.setMobile(mobile.trim());
            newUser.setPasswordHash(passwordHash);
            newUser.setSalt(salt);
            newUser.setRole("SELLER".equalsIgnoreCase(role) ? "SELLER" : "BUYER");
            newUser.setStatus("ACTIVE");

            int userId = userDAO.create(newUser);
            if (userId > 0) {
                newUser.setUserId(userId);

                // Welcome notification
                notificationDAO.create(userId, "Welcome to CouponLoop!",
                        "Your account is ready. Discover incredible deals or list unused vouchers to earn cash.", "SUCCESS");

                // Auto login after registration
                HttpSession session = req.getSession(true);
                session.setAttribute("user", newUser);
                session.setAttribute("userId", userId);
                session.setAttribute("userRole", newUser.getRole());

                Map<String, Object> data = new HashMap<>();
                data.put("userId", userId);
                data.put("fullName", newUser.getFullName());
                data.put("email", newUser.getEmail());
                data.put("role", newUser.getRole());

                JsonUtil.sendSuccess(resp, "Account created successfully! Welcome to CouponLoop.", data);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create account. Please try again.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String email = json.has("email") ? json.get("email").getAsString() : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";

        if (!ValidationUtil.isNotEmpty(email) || !ValidationUtil.isNotEmpty(password)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required.");
            return;
        }

        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password.");
                return;
            }

            if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Your account has been suspended by administration.");
                return;
            }

            boolean matches = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());
            if (!matches) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password.");
                return;
            }

            // Create new clean session
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userRole", user.getRole());

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("fullName", user.getFullName());
            data.put("email", user.getEmail());
            data.put("role", user.getRole());
            data.put("isAdmin", user.isAdmin());

            JsonUtil.sendSuccess(resp, "Login successful! Welcome back, " + user.getFullName() + ".", data);

        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendSuccess(resp, "Logged out successfully.");
    }

    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String email = json.has("email") ? json.get("email").getAsString() : "";

        if (!ValidationUtil.isValidEmail(email)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid email address.");
            return;
        }

        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                // For security reasons, still return generic friendly response
                JsonUtil.sendSuccess(resp, "If this email is registered, password reset instructions have been generated.", Map.of("simulatedToken", "DEMO_TOKEN_123456"));
                return;
            }

            // Generate demo simulation reset token
            String simulatedToken = "RESET_" + Math.abs(email.hashCode());
            Map<String, Object> data = new HashMap<>();
            data.put("email", user.getEmail());
            data.put("simulatedToken", simulatedToken);
            data.put("instructions", "Use the reset code below to update your password in this demo sandbox.");

            JsonUtil.sendSuccess(resp, "Password recovery instructions generated.", data);

        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String email = json.has("email") ? json.get("email").getAsString() : "";
        String newPassword = json.has("newPassword") ? json.get("newPassword").getAsString() : "";
        String confirmPassword = json.has("confirmPassword") ? json.get("confirmPassword").getAsString() : "";

        if (!ValidationUtil.isValidPassword(newPassword) || !newPassword.equals(confirmPassword)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters and match confirmation.");
            return;
        }

        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User account not found.");
                return;
            }

            String newSalt = PasswordUtil.generateSalt();
            String newHash = PasswordUtil.hashPassword(newPassword, newSalt);

            boolean updated = userDAO.updatePassword(user.getUserId(), newHash, newSalt);
            if (updated) {
                notificationDAO.create(user.getUserId(), "Password Reset Successfully",
                        "Your account password was updated. If you did not make this change, contact support immediately.", "WARNING");
                JsonUtil.sendSuccess(resp, "Password has been reset successfully. You can now login with your new password.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to reset password.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
