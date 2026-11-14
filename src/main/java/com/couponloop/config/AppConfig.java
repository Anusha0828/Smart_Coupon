package com.couponloop.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Global application configuration holder.
 */
public class AppConfig {

    private static double commissionRate = 10.0;
    private static String appName = "CouponLoop";
    private static String appVersion = "1.0.0";

    static {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
                String rate = props.getProperty("platform.commission.rate");
                if (rate != null) {
                    commissionRate = Double.parseDouble(rate.trim());
                }
                appName = props.getProperty("app.name", "CouponLoop");
                appVersion = props.getProperty("app.version", "1.0.0");
            }
        } catch (Exception e) {
            System.err.println("Notice: using default configuration values: " + e.getMessage());
        }
    }

    public static double getCommissionRate() {
        return commissionRate;
    }

    public static String getAppName() {
        return appName;
    }

    public static String getAppVersion() {
        return appVersion;
    }
}
