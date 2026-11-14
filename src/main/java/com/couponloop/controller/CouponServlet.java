package com.couponloop.controller;

import com.couponloop.dao.CategoryDAO;
import com.couponloop.dao.CouponDAO;
import com.couponloop.model.Category;
import com.couponloop.model.Coupon;
import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
import com.couponloop.util.SmartPricingUtil;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/coupons/*")
public class CouponServlet extends HttpServlet {

    private final CouponDAO couponDAO = new CouponDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    public void init() throws ServletException {
        // Automatically check and expire passed coupons on servlet startup
        couponDAO.markExpiredCoupons();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        Integer currentUserId = getLoggedInUserId(req);

        try {
            if (path == null || "/".equals(path) || "/search".equals(path)) {
                handleSearch(req, resp, currentUserId);
            } else if ("/details".equals(path)) {
                handleDetails(req, resp, currentUserId);
            } else if ("/recommendations".equals(path)) {
                handleRecommendations(req, resp, currentUserId);
            } else if ("/categories".equals(path)) {
                handleCategories(req, resp);
            } else if ("/brands".equals(path)) {
                handleBrands(req, resp);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/smart-pricing".equals(path)) {
            handleSmartPricing(req, resp);
        } else if ("/eligibility-check".equals(path)) {
            handleEligibilityCheck(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown POST endpoint");
        }
    }

    private void handleSearch(HttpServletRequest req, HttpServletResponse resp, Integer currentUserId)
            throws SQLException, IOException {
        String keyword = req.getParameter("q");
        String catParam = req.getParameter("category");
        Integer categoryId = null;
        if (catParam != null && !catParam.trim().isEmpty()) {
            try {
                categoryId = Integer.parseInt(catParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        String brand = req.getParameter("brand");
        String minPriceStr = req.getParameter("minPrice");
        Double minPrice = (minPriceStr != null && !minPriceStr.trim().isEmpty()) ? Double.parseDouble(minPriceStr) : null;

        String maxPriceStr = req.getParameter("maxPrice");
        Double maxPrice = (maxPriceStr != null && !maxPriceStr.trim().isEmpty()) ? Double.parseDouble(maxPriceStr) : null;

        String daysStr = req.getParameter("expiringDays");
        Integer expiringDays = (daysStr != null && !daysStr.trim().isEmpty()) ? Integer.parseInt(daysStr) : null;

        String sortBy = req.getParameter("sort");
        String limitStr = req.getParameter("limit");
        int limit = (limitStr != null) ? Integer.parseInt(limitStr) : 24;

        String offsetStr = req.getParameter("offset");
        int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;

        List<Coupon> list = couponDAO.searchCoupons(keyword, categoryId, brand, minPrice, maxPrice, expiringDays, sortBy, limit, offset, currentUserId);
        JsonUtil.sendSuccess(resp, "Coupons retrieved successfully", list);
    }

    private void handleDetails(HttpServletRequest req, HttpServletResponse resp, Integer currentUserId)
            throws SQLException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing coupon ID");
            return;
        }

        int couponId = Integer.parseInt(idStr.trim());
        couponDAO.incrementViews(couponId);
        Coupon coupon = couponDAO.findById(couponId, currentUserId);

        if (coupon == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Coupon not found");
            return;
        }

        JsonUtil.sendSuccess(resp, "Coupon details retrieved", coupon);
    }

    private void handleRecommendations(HttpServletRequest req, HttpServletResponse resp, Integer currentUserId)
            throws SQLException, IOException {
        String limitStr = req.getParameter("limit");
        int limit = (limitStr != null) ? Integer.parseInt(limitStr) : 8;
        List<Coupon> list = couponDAO.getRecommendations(currentUserId, limit);
        JsonUtil.sendSuccess(resp, "Recommendations retrieved", list);
    }

    private void handleCategories(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        List<Category> list = categoryDAO.findAllActive();
        JsonUtil.sendSuccess(resp, "Categories retrieved", list);
    }

    private void handleBrands(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        List<String> list = couponDAO.getDistinctBrands();
        JsonUtil.sendSuccess(resp, "Brands retrieved", list);
    }

    private void handleSmartPricing(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        double originalValue = json.has("originalValue") ? json.get("originalValue").getAsDouble() : 0.0;
        String expiryDateStr = json.has("expiryDate") ? json.get("expiryDate").getAsString() : null;
        String brandName = json.has("brandName") ? json.get("brandName").getAsString() : "";

        long daysToExpiry = 15;
        if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
            try {
                LocalDate exp = LocalDate.parse(expiryDateStr.trim());
                daysToExpiry = Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), exp));
            } catch (Exception ignored) {}
        }

        SmartPricingUtil.PricingResult result = SmartPricingUtil.calculateSuggestion(originalValue, daysToExpiry, brandName);
        JsonUtil.sendSuccess(resp, "Smart pricing calculated", result);
    }

    private void handleEligibilityCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.parseJsonObject(req);
        String sourcePlatform = json.has("sourcePlatform") ? json.get("sourcePlatform").getAsString() : "";
        String couponCode = json.has("couponCode") ? json.get("couponCode").getAsString() : "";
        String expiryDateStr = json.has("expiryDate") ? json.get("expiryDate").getAsString() : null;
        boolean isAccountBound = json.has("isAccountBound") && json.get("isAccountBound").getAsBoolean();
        boolean isCashbackReward = json.has("isCashbackReward") && json.get("isCashbackReward").getAsBoolean();
        boolean hasTransferRight = json.has("hasTransferRight") && json.get("hasTransferRight").getAsBoolean();

        Map<String, Object> evaluation = new HashMap<>();
        boolean eligible = true;
        StringBuilder feedback = new StringBuilder();

        if (isAccountBound || isCashbackReward) {
            eligible = false;
            feedback.append("Direct account-linked cashback rewards or non-transferable personalized offers are prohibited. ");
        }

        if (!hasTransferRight) {
            eligible = false;
            feedback.append("You must have explicit transferability rights under the issuing merchant terms. ");
        }

        if (expiryDateStr != null) {
            try {
                LocalDate exp = LocalDate.parse(expiryDateStr);
                if (!ValidationUtil.isFutureDate(exp)) {
                    eligible = false;
                    feedback.append("Voucher is expired or expires today. Coupons must have future validity. ");
                }
            } catch (Exception e) {
                eligible = false;
                feedback.append("Invalid expiry date format. ");
            }
        }

        if (eligible) {
            feedback.append("Coupon meets CouponLoop transferability criteria! Source '").append(sourcePlatform)
                    .append("' serves as coupon origin label.");
        }

        evaluation.put("eligible", eligible);
        evaluation.put("reason", feedback.toString());
        evaluation.put("complianceStatus", eligible ? "COMPLIANT" : "REJECTED_NON_TRANSFERABLE");

        JsonUtil.sendSuccess(resp, "Coupon eligibility evaluation complete", evaluation);
    }

    private Integer getLoggedInUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User u = (User) session.getAttribute("user");
            return u.getUserId();
        }
        return null;
    }
}
