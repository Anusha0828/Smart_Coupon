package com.couponloop.model;

import java.sql.Timestamp;

public class Payment {
    private int paymentId;
    private String transactionId;
    private int userId;
    private double amount;
    private String paymentMethod;
    private String paymentStatus; // SUCCESS, FAILED, REFUNDED
    private String gatewayPayload;
    private Timestamp createdAt;

    public Payment() {}

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getGatewayPayload() { return gatewayPayload; }
    public void setGatewayPayload(String gatewayPayload) { this.gatewayPayload = gatewayPayload; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
