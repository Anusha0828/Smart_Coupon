package com.couponloop.controller;

import com.couponloop.dao.NotificationDAO;
import com.couponloop.dao.PurchaseDAO;
import com.couponloop.dao.ReviewDAO;
import com.couponloop.model.Purchase;
import com.couponloop.model.Review;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/reviews/*")
public class ReviewServlet extends HttpServlet {

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sellerIdStr = req.getParameter("sellerId");
        if (sellerIdStr == null || sellerIdStr.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing sellerId");
            return;
        }

        try {
            int sellerId = Integer.parseInt(sellerIdStr.trim());
            List<Review> list = reviewDAO.findBySellerId(sellerId);
            double avg = reviewDAO.getSellerAverageRating(sellerId);

            Map<String, Object> map = new HashMap<>();
            map.put("reviews", list);
            map.put("averageRating", avg);
            map.put("totalReviews", list.size());

            JsonUtil.sendSuccess(resp, "Seller reviews retrieved", map);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required to submit review");
            return;
        }

        JsonObject json = JsonUtil.parseJsonObject(req);
        int purchaseId = json.has("purchaseId") ? json.get("purchaseId").getAsInt() : 0;
        int rating = json.has("rating") ? json.get("rating").getAsInt() : 5;
        String reviewText = json.has("reviewText") ? json.get("reviewText").getAsString() : "";

        if (purchaseId <= 0 || rating < 1 || rating > 5) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid purchase ID and star rating (1-5) required.");
            return;
        }

        try {
            Purchase purchase = purchaseDAO.findPurchaseById(purchaseId, user.getUserId());
            if (purchase == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "You can only review coupons you have purchased.");
                return;
            }

            Review existing = reviewDAO.findByPurchaseId(purchaseId);
            if (existing != null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "You have already reviewed this purchase.");
                return;
            }

            Review review = new Review();
            review.setPurchaseId(purchaseId);
            review.setBuyerId(user.getUserId());
            // Lookup seller from coupon
            com.couponloop.dao.CouponDAO couponDAO = new com.couponloop.dao.CouponDAO();
            com.couponloop.model.Coupon coupon = couponDAO.findByIdRaw(purchase.getCouponId());
            if (coupon == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Original coupon record missing.");
                return;
            }

            review.setSellerId(coupon.getSellerId());
            review.setRating(rating);
            review.setReviewText(ValidationUtil.sanitize(reviewText));

            boolean created = reviewDAO.create(review);
            if (created) {
                notificationDAO.create(coupon.getSellerId(), "New Review Received",
                        String.format("Buyer left a %d-star rating on your listing '%s'.", rating, coupon.getTitle()), "SUCCESS");
                JsonUtil.sendSuccess(resp, "Thank you! Your seller review has been submitted.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit review.");
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
