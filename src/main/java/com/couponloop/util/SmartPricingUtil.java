package com.couponloop.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calculates dynamic smart pricing suggestions for coupon listings.
 * Balances face value, remaining days to expiry, and brand demand index.
 */
public class SmartPricingUtil {

    private static final Set<String> HIGH_DEMAND_BRANDS = Set.of(
            "amazon", "swiggy", "zomato", "myntra", "makemytrip", "bookmyshow",
            "blinkit", "uber", "nykaa", "dominos", "flipkart"
    );

    public static class PricingResult {
        public double suggestedPrice;
        public double minRecommended;
        public double maxRecommended;
        public double discountPercentage;
        public String rationale;

        public PricingResult(double suggestedPrice, double minRecommended, double maxRecommended, double discountPercentage, String rationale) {
            this.suggestedPrice = Math.round(suggestedPrice * 100.0) / 100.0;
            this.minRecommended = Math.round(minRecommended * 100.0) / 100.0;
            this.maxRecommended = Math.round(maxRecommended * 100.0) / 100.0;
            this.discountPercentage = Math.round(discountPercentage * 10.0) / 10.0;
            this.rationale = rationale;
        }
    }

    public static PricingResult calculateSuggestion(double originalValue, long daysToExpiry, String brandName) {
        if (originalValue <= 0) {
            return new PricingResult(0, 0, 0, 0, "Invalid original coupon value");
        }

        // Base benchmark: coupons typically trade around 28% of face value
        double baseRate = 0.28;

        // Brand popularity multiplier
        double brandFactor = 1.0;
        String normalizedBrand = brandName != null ? brandName.trim().toLowerCase() : "";
        if (HIGH_DEMAND_BRANDS.contains(normalizedBrand)) {
            brandFactor = 1.20; // 20% higher demand for popular household brands
        }

        // Expiry urgency multiplier
        double expiryFactor;
        String urgencyNote;
        if (daysToExpiry >= 30) {
            expiryFactor = 1.15;
            urgencyNote = "Long validity gives you optimal pricing leverage.";
        } else if (daysToExpiry >= 14) {
            expiryFactor = 1.0;
            urgencyNote = "Standard validity window.";
        } else if (daysToExpiry >= 7) {
            expiryFactor = 0.85;
            urgencyNote = "Expiring in under two weeks; competitive pricing will attract faster buyers.";
        } else if (daysToExpiry >= 3) {
            expiryFactor = 0.70;
            urgencyNote = "Expiring soon (< 7 days); discounted rate recommended for rapid liquidation.";
        } else {
            expiryFactor = 0.50;
            urgencyNote = "Expiring within 72 hours; clearance pricing recommended to prevent total loss.";
        }

        double calculatedRate = baseRate * brandFactor * expiryFactor;

        // Ensure rate stays between 15% and 60% of original value
        if (calculatedRate < 0.15) calculatedRate = 0.15;
        if (calculatedRate > 0.60) calculatedRate = 0.60;

        double suggested = originalValue * calculatedRate;
        if (suggested < 10.0 && originalValue >= 20.0) {
            suggested = 10.0;
        }

        double minRec = Math.max(5.0, suggested * 0.80);
        double maxRec = Math.min(originalValue * 0.90, suggested * 1.25);
        double buyerDiscount = ((originalValue - suggested) / originalValue) * 100.0;

        String rationale = String.format("Suggested at ₹%.0f (%.0f%% buyer discount). %s Brand demand rating: %s.",
                suggested, buyerDiscount, urgencyNote, (brandFactor > 1.0 ? "High" : "Standard"));

        return new PricingResult(suggested, minRec, maxRec, buyerDiscount, rationale);
    }
}
