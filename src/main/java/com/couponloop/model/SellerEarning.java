package com.couponloop.model;

import java.sql.Timestamp;

public class SellerEarning {
    private int earningId;
    private int sellerId;
    private int couponId;
    private String couponTitle;
    private String brandName;
    private int purchaseId;
    private double grossAmount;
    private double commissionRate;
    private double commissionAmount;
    private double netEarning;
    private String payoutStatus; // PENDING, PAID, CANCELLED
    private Timestamp payoutDate;
    private Timestamp createdAt;

    public SellerEarning() {}

    public int getEarningId() { return earningId; }
    public void setEarningId(int earningId) { this.earningId = earningId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }

    public String getCouponTitle() { return couponTitle; }
    public void setCouponTitle(String couponTitle) { this.couponTitle = couponTitle; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }

    public double getCommissionRate() { return commissionRate; }
    public void setCommissionRate(double commissionRate) { this.commissionRate = commissionRate; }

    public double getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(double commissionAmount) { this.commissionAmount = commissionAmount; }

    public double getNetEarning() { return netEarning; }
    public void setNetEarning(double netEarning) { this.netEarning = netEarning; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public Timestamp getPayoutDate() { return payoutDate; }
    public void setPayoutDate(Timestamp payoutDate) { this.payoutDate = payoutDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
