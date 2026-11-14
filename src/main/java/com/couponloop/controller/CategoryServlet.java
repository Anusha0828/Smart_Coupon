package com.couponloop.controller;

import com.couponloop.dao.CategoryDAO;
import com.couponloop.model.Category;
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

@WebServlet("/api/category/*")
public class CategoryServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Category> list = categoryDAO.findAll();
            JsonUtil.sendSuccess(resp, "Categories retrieved", list);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null || !user.isAdmin()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
            return;
        }

        String path = req.getPathInfo();
        JsonObject json = JsonUtil.parseJsonObject(req);

        try {
            if ("/create".equals(path)) {
                String name = json.has("name") ? json.get("name").getAsString() : "";
                String slug = json.has("slug") ? json.get("slug").getAsString() : "";
                String desc = json.has("description") ? json.get("description").getAsString() : "";
                String icon = json.has("iconClass") ? json.get("iconClass").getAsString() : "fa-solid fa-tag";

                if (!ValidationUtil.isNotEmpty(name) || !ValidationUtil.isNotEmpty(slug)) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name and slug are required");
                    return;
                }

                Category c = new Category(0, ValidationUtil.sanitize(name), slug.toLowerCase().trim(), desc, icon, true);
                int id = categoryDAO.create(c);
                if (id > 0) {
                    JsonUtil.sendSuccess(resp, "Category created successfully");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create category");
                }
            } else if ("/toggle".equals(path)) {
                int catId = json.has("categoryId") ? json.get("categoryId").getAsInt() : 0;
                boolean active = json.has("active") && json.get("active").getAsBoolean();
                categoryDAO.toggleActive(catId, active);
                JsonUtil.sendSuccess(resp, "Category status updated");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown category endpoint");
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
