package com.couponloop.filter;

import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter({"/api/buyer/*", "/api/seller/*", "/api/wishlist/*", "/api/user/*", "/api/purchase/*", "/api/disputes/raise",
            "/buyer-dashboard.html", "/seller-dashboard.html", "/upload-coupon.html", "/wishlist.html", "/profile.html"})
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String uri = req.getRequestURI();

        if (user == null) {
            if (uri.contains("/api/")) {
                JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in to access this feature.");
            } else {
                res.sendRedirect(req.getContextPath() + "/login.html?redirect=" + req.getServletPath());
            }
            return;
        }

        // Suspended user check
        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            if (uri.contains("/api/")) {
                JsonUtil.sendError(res, HttpServletResponse.SC_FORBIDDEN, "Your account has been suspended by administration.");
            } else {
                res.sendRedirect(req.getContextPath() + "/login.html?error=account_suspended");
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
