package com.couponloop.model;

import java.sql.Timestamp;

public class Dispute {
    private int disputeId;
    private int purchaseId;
    private int couponId;
    private String couponTitle;
    private int buyerId;
    private String buyerName;
    private int sellerId;
    private String sellerName;
    private String reason;
    private String details;
    private String evidenceUrl;
    private String status; // OPEN, UNDER_REVIEW, RESOLVED_REFUND, RESOLVED_REJECTED
    private String adminNotes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Dispute() {}

    public int getDisputeId() { return disputeId; }
    public void setDisputeId(int disputeId) { this.disputeId = disputeId; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }

    public String getCouponTitle() { return couponTitle; }
    public void setCouponTitle(String couponTitle) { this.couponTitle = couponTitle; }

    public int getBuyerId() { return buyerId; }
    public void setBuyerId(int buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
