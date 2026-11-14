package com.couponloop.controller;

import com.couponloop.dao.NotificationDAO;
import com.couponloop.model.Notification;
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
import java.util.List;
import java.util.Map;

@WebServlet("/api/notifications/*")
public class NotificationServlet extends HttpServlet {

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
            if ("/unread-count".equals(path)) {
                int count = notificationDAO.getUnreadCount(user.getUserId());
                JsonUtil.sendSuccess(resp, "Unread count", Map.of("unreadCount", count));
            } else {
                List<Notification> list = notificationDAO.findByUserId(user.getUserId());
                JsonUtil.sendSuccess(resp, "Notifications retrieved", list);
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
            if ("/mark-read".equals(path)) {
                JsonObject json = JsonUtil.parseJsonObject(req);
                int notifId = json.has("notificationId") ? json.get("notificationId").getAsInt() : 0;
                notificationDAO.markAsRead(notifId, user.getUserId());
                JsonUtil.sendSuccess(resp, "Notification marked as read");
            } else if ("/mark-all-read".equals(path)) {
                notificationDAO.markAllAsRead(user.getUserId());
                JsonUtil.sendSuccess(resp, "All notifications marked as read");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown notification action");
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
