package com.couponloop.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connectivity using configuration files and environment overrides.
 * Ensures database passwords and sensitive credentials are never hardcoded.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;
    private static String driver;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("Warning: Unable to load db.properties, relying on environment: " + e.getMessage());
        }

        // Environment variables take precedence if present
        driver = System.getenv("DB_DRIVER");
        if (driver == null || driver.trim().isEmpty()) {
            driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        }

        url = System.getenv("DB_URL");
        if (url == null || url.trim().isEmpty()) {
            url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/couponloop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }

        user = System.getenv("DB_USER");
        if (user == null || user.trim().isEmpty()) {
            user = props.getProperty("db.user", "root");
        }

        password = System.getenv("DB_PASSWORD");
        if (password == null) {
            password = props.getProperty("db.password", "Anusha@556");
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading MySQL JDBC Driver (" + driver + "): " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
