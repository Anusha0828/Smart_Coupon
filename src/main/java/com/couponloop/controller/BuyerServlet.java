package com.couponloop.controller;

import com.couponloop.dao.PurchaseDAO;
import com.couponloop.dao.WishlistDAO;
import com.couponloop.model.Purchase;
import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
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

@WebServlet("/api/buyer/*")
public class BuyerServlet extends HttpServlet {

    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final WishlistDAO wishlistDAO = new WishlistDAO();

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
            } else if ("/purchases".equals(path)) {
                handlePurchases(req, resp, user);
            } else if ("/purchase-details".equals(path)) {
                handlePurchaseDetails(req, resp, user);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown buyer endpoint");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleDashboard(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        List<Purchase> purchases = purchaseDAO.findPurchasesByBuyer(user.getUserId());
        int totalPurchases = purchases.size();
        double totalSpent = 0;
        for (Purchase p : purchases) {
            totalSpent += p.getPurchasePrice();
        }

        int wishlistCount = wishlistDAO.getWishlistByUser(user.getUserId()).size();

        Map<String, Object> map = new HashMap<>();
        map.put("totalPurchases", totalPurchases);
        map.put("totalSpent", Math.round(totalSpent * 100.0) / 100.0);
        map.put("wishlistCount", wishlistCount);
        map.put("recentPurchases", purchases.size() > 5 ? purchases.subList(0, 5) : purchases);

        JsonUtil.sendSuccess(resp, "Buyer dashboard loaded", map);
    }

    private void handlePurchases(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        List<Purchase> list = purchaseDAO.findPurchasesByBuyer(user.getUserId());
        JsonUtil.sendSuccess(resp, "Purchases retrieved", list);
    }

    private void handlePurchaseDetails(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing purchase ID");
            return;
        }

        int purchaseId = Integer.parseInt(idStr.trim());
        Purchase p = purchaseDAO.findPurchaseById(purchaseId, user.getUserId());
        if (p == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Purchase record not found or unauthorized access.");
            return;
        }

        JsonUtil.sendSuccess(resp, "Purchase details retrieved", p);
    }

    private User getAuthenticatedUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
}
