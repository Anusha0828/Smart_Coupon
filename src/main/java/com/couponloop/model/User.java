package com.couponloop.model;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String fullName;
    private String email;
    private String mobile;
    private String passwordHash;
    private String salt;
    private String role; // BUYER, SELLER, ADMIN
    private String status; // ACTIVE, SUSPENDED
    private String upiId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {}

    public User(int userId, String fullName, String email, String mobile, String role, String status, String upiId) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.status = status;
        this.upiId = upiId;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
