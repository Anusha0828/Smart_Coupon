package com.couponloop.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Coupon {
    private int couponId;
    private int sellerId;
    private int categoryId;
    private String categoryName;
    private String categorySlug;
    private String sellerName;
    private String sellerEmail;
    private double sellerRating;
    private String title;
    private String brandName;
    private String sourcePlatform;
    private String couponCode; // Hidden unless purchased or viewing own listing
    private String description;
    private double originalValue;
    private double sellingPrice;
    private double minPurchaseAmount;
    private Date expiryDate;
    private String usageConditions;
    private String imageUrl;
    private boolean transferableConfirmed;
    private boolean termsConfirmed;
    private Integer adminId;
    private Timestamp approvedAt;
    private String rejectionReason;
    private String status; // PENDING, APPROVED, ACTIVE, SOLD, EXPIRED, REJECTED, DISPUTED
    private int viewsCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Computed helpers
    private long daysToExpiry;
    private double discountPercentage;
    private double savingsAmount;
    private boolean inWishlist;

    public Coupon() {}

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public double getSellerRating() { return sellerRating; }
    public void setSellerRating(double sellerRating) { this.sellerRating = sellerRating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getOriginalValue() { return originalValue; }
    public void setOriginalValue(double originalValue) { 
        this.originalValue = originalValue; 
        calculateSavings();
    }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { 
        this.sellingPrice = sellingPrice; 
        calculateSavings();
    }

    public double getMinPurchaseAmount() { return minPurchaseAmount; }
    public void setMinPurchaseAmount(double minPurchaseAmount) { this.minPurchaseAmount = minPurchaseAmount; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { 
        this.expiryDate = expiryDate; 
        if (expiryDate != null) {
            LocalDate exp = expiryDate.toLocalDate();
            LocalDate today = LocalDate.now();
            this.daysToExpiry = ChronoUnit.DAYS.between(today, exp);
        }
    }

    public String getUsageConditions() { return usageConditions; }
    public void setUsageConditions(String usageConditions) { this.usageConditions = usageConditions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isTransferableConfirmed() { return transferableConfirmed; }
    public void setTransferableConfirmed(boolean transferableConfirmed) { this.transferableConfirmed = transferableConfirmed; }

    public boolean isTermsConfirmed() { return termsConfirmed; }
    public void setTermsConfirmed(boolean termsConfirmed) { this.termsConfirmed = termsConfirmed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public long getDaysToExpiry() { return daysToExpiry; }
    public void setDaysToExpiry(long daysToExpiry) { this.daysToExpiry = daysToExpiry; }

    public double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }

    public double getSavingsAmount() { return savingsAmount; }
    public void setSavingsAmount(double savingsAmount) { this.savingsAmount = savingsAmount; }

    public boolean isInWishlist() { return inWishlist; }
    public void setInWishlist(boolean inWishlist) { this.inWishlist = inWishlist; }

    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }

    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    public Timestamp getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Timestamp approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public void calculateSavings() {
        if (originalValue > 0) {
            this.savingsAmount = Math.max(0, originalValue - sellingPrice);
            this.discountPercentage = Math.round(((originalValue - sellingPrice) / originalValue) * 100.0 * 10.0) / 10.0;
        }
    }
}
