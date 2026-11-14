package com.couponloop.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*")
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpServletResponse httpRes = (HttpServletResponse) response;
        HttpServletRequest httpReq = (HttpServletRequest) request;

        // Add security and CORS headers for local testing
        httpRes.setHeader("X-Content-Type-Options", "nosniff");
        httpRes.setHeader("X-Frame-Options", "SAMEORIGIN");
        httpRes.setHeader("X-XSS-Protection", "1; mode=block");

        chain.doFilter(request, response);
    }
}
