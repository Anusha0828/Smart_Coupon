-- =======================================================================
-- CouponLoop Database Schema
-- Database Name: couponloop_db
-- =======================================================================

CREATE DATABASE IF NOT EXISTS couponloop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE couponloop_db;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    mobile VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role ENUM('BUYER', 'SELLER', 'ADMIN') NOT NULL DEFAULT 'BUYER',
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    upi_id VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role),
    INDEX idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. COUPON CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS coupon_categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_class VARCHAR(50) DEFAULT 'fa-solid fa-tag',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cat_slug (slug),
    INDEX idx_cat_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. COUPONS TABLE
CREATE TABLE IF NOT EXISTS coupons (
    coupon_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    category_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    brand_name VARCHAR(100) NOT NULL,
    source_platform VARCHAR(100) NOT NULL,
    coupon_code VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    original_value DECIMAL(10,2) NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    min_purchase_amount DECIMAL(10,2) DEFAULT 0.00,
    expiry_date DATE NOT NULL,
    usage_conditions TEXT,
    image_url VARCHAR(500),
    is_transferable_confirmed BOOLEAN DEFAULT FALSE,
    terms_confirmed BOOLEAN DEFAULT FALSE,
    admin_id INT NULL,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    status ENUM('PENDING', 'APPROVED', 'ACTIVE', 'SOLD', 'EXPIRED', 'REJECTED', 'DISPUTED') DEFAULT 'PENDING',
    views_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES coupon_categories(category_id),
    FOREIGN KEY (admin_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_coupon_seller (seller_id),
    INDEX idx_coupon_category (category_id),
    INDEX idx_coupon_status (status),
    INDEX idx_coupon_expiry (expiry_date),
    INDEX idx_coupon_price (selling_price),
    INDEX idx_coupon_brand (brand_name),
    INDEX idx_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. PAYMENTS TABLE
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status ENUM('SUCCESS', 'FAILED', 'REFUNDED') DEFAULT 'SUCCESS',
    gateway_payload TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_pay_tx (transaction_id),
    INDEX idx_pay_user (user_id),
    INDEX idx_pay_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. PURCHASES TABLE
CREATE TABLE IF NOT EXISTS purchases (
    purchase_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    coupon_id INT NOT NULL,
    payment_id INT NOT NULL,
    purchase_price DECIMAL(10,2) NOT NULL,
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_revealed BOOLEAN DEFAULT TRUE,
    dispute_status ENUM('NONE', 'RAISED', 'RESOLVED', 'REFUNDED') DEFAULT 'NONE',
    FOREIGN KEY (buyer_id) REFERENCES users(user_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id),
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    INDEX idx_pur_buyer (buyer_id),
    INDEX idx_pur_coupon (coupon_id),
    INDEX idx_pur_dispute (dispute_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. SELLER EARNINGS TABLE
CREATE TABLE IF NOT EXISTS seller_earnings (
    earning_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    coupon_id INT NOT NULL,
    purchase_id INT NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    commission_rate DECIMAL(5,2) DEFAULT 10.00,
    commission_amount DECIMAL(10,2) NOT NULL,
    net_earning DECIMAL(10,2) NOT NULL,
    payout_status ENUM('PENDING', 'PAID', 'CANCELLED') DEFAULT 'PENDING',
    payout_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(user_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id),
    FOREIGN KEY (purchase_id) REFERENCES purchases(purchase_id),
    INDEX idx_earn_seller (seller_id),
    INDEX idx_earn_payout (payout_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. WISHLIST TABLE
CREATE TABLE IF NOT EXISTS wishlist (
    wishlist_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    coupon_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_coupon (user_id, coupon_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id) ON DELETE CASCADE,
    INDEX idx_wish_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. REVIEWS TABLE
CREATE TABLE IF NOT EXISTS reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL UNIQUE,
    buyer_id INT NOT NULL,
    seller_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (purchase_id) REFERENCES purchases(purchase_id),
    FOREIGN KEY (buyer_id) REFERENCES users(user_id),
    FOREIGN KEY (seller_id) REFERENCES users(user_id),
    INDEX idx_rev_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'EXPIRY', 'PURCHASE', 'DISPUTE') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. DISPUTES TABLE
CREATE TABLE IF NOT EXISTS disputes (
    dispute_id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    coupon_id INT NOT NULL,
    buyer_id INT NOT NULL,
    seller_id INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT NOT NULL,
    evidence_url VARCHAR(500),
    status ENUM('OPEN', 'UNDER_REVIEW', 'RESOLVED_REFUND', 'RESOLVED_REJECTED') DEFAULT 'OPEN',
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (purchase_id) REFERENCES purchases(purchase_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id),
    FOREIGN KEY (buyer_id) REFERENCES users(user_id),
    FOREIGN KEY (seller_id) REFERENCES users(user_id),
    INDEX idx_disp_status (status),
    INDEX idx_disp_buyer (buyer_id),
    INDEX idx_disp_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. COUPON REPORTS TABLE
CREATE TABLE IF NOT EXISTS coupon_reports (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    coupon_id INT NOT NULL,
    reported_by_user_id INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status ENUM('PENDING', 'REVIEWED', 'DISMISSED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id) ON DELETE CASCADE,
    FOREIGN KEY (reported_by_user_id) REFERENCES users(user_id),
    INDEX idx_rep_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. ADMIN ACTIONS TABLE
CREATE TABLE IF NOT EXISTS admin_actions (
    action_id INT AUTO_INCREMENT PRIMARY KEY,
    admin_id INT NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id INT NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(user_id),
    INDEX idx_act_admin (admin_id),
    INDEX idx_act_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
