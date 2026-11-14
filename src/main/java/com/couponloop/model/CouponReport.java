package com.couponloop.model;

import java.sql.Timestamp;

public class CouponReport {
    private int reportId;
    private int couponId;
    private String couponTitle;
    private int reportedByUserId;
    private String reporterName;
    private String reason;
    private String details;
    private String status; // PENDING, REVIEWED, DISMISSED
    private Timestamp createdAt;

    public CouponReport() {}

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }

    public String getCouponTitle() { return couponTitle; }
    public void setCouponTitle(String couponTitle) { this.couponTitle = couponTitle; }

    public int getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(int reportedByUserId) { this.reportedByUserId = reportedByUserId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
