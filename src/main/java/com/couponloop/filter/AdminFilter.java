package com.couponloop.filter;

import com.couponloop.model.User;
import com.couponloop.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter({"/admin/*", "/api/admin/*", "/admin-dashboard.html"})
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String uri = req.getRequestURI();

        if (user == null || !user.isAdmin()) {
            if (uri.contains("/api/")) {
                JsonUtil.sendError(res, HttpServletResponse.SC_FORBIDDEN, "Administrator privileges required.");
            } else {
                res.sendRedirect(req.getContextPath() + "/login.html?error=admin_required");
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
