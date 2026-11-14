package com.couponloop.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility for cryptographic salt generation and password hashing using SHA-256.
 * Enforces secure password handling across registration and login.
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            byte[] pwdBytes = password.getBytes(StandardCharsets.UTF_8);

            byte[] combined = new byte[saltBytes.length + pwdBytes.length];
            System.arraycopy(saltBytes, 0, combined, 0, saltBytes.length);
            System.arraycopy(pwdBytes, 0, combined, saltBytes.length, pwdBytes.length);

            byte[] hashed = md.digest(combined);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        String calculated = hashPassword(password, salt);
        return calculated.equalsIgnoreCase(expectedHash);
    }
}
