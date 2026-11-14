package com.couponloop.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validates user input and coupon listing constraints on both backend and business layers.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidMobile(String mobile) {
        if (mobile == null) return false;
        String cleaned = mobile.replaceAll("[^0-9]", "");
        return MOBILE_PATTERN.matcher(cleaned).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidCouponPrice(double originalValue, double sellingPrice) {
        if (sellingPrice <= 0 || originalValue <= 0) {
            return false;
        }
        return sellingPrice <= originalValue;
    }

    public static boolean isFutureDate(LocalDate date) {
        if (date == null) return false;
        return date.isAfter(LocalDate.now());
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.replace("<", "&lt;").replace(">", "&gt;").trim();
    }
}
