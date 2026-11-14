package com.couponloop.controller;

import com.couponloop.dao.WishlistDAO;
import com.couponloop.model.User;
import com.couponloop.model.Wishlist;
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

@WebServlet("/api/wishlist/*")
public class WishlistServlet extends HttpServlet {

    private final WishlistDAO wishlistDAO = new WishlistDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        try {
            List<Wishlist> list = wishlistDAO.getWishlistByUser(user.getUserId());
            JsonUtil.sendSuccess(resp, "Wishlist retrieved", list);
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
        JsonObject json = JsonUtil.parseJsonObject(req);
        int couponId = json.has("couponId") ? json.get("couponId").getAsInt() : 0;

        if (couponId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid coupon ID required");
            return;
        }

        try {
            if ("/toggle".equals(path)) {
                boolean inWishlist = wishlistDAO.isInWishlist(user.getUserId(), couponId);
                if (inWishlist) {
                    wishlistDAO.remove(user.getUserId(), couponId);
                    Map<String, Object> data = Map.of("inWishlist", false);
                    JsonUtil.sendSuccess(resp, "Removed from wishlist", data);
                } else {
                    wishlistDAO.add(user.getUserId(), couponId);
                    Map<String, Object> data = Map.of("inWishlist", true);
                    JsonUtil.sendSuccess(resp, "Added to wishlist", data);
                }
            } else if ("/add".equals(path)) {
                wishlistDAO.add(user.getUserId(), couponId);
                JsonUtil.sendSuccess(resp, "Added to wishlist");
            } else if ("/remove".equals(path)) {
                wishlistDAO.remove(user.getUserId(), couponId);
                JsonUtil.sendSuccess(resp, "Removed from wishlist");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown wishlist action");
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
