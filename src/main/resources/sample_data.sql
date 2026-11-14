-- =======================================================================
-- CouponLoop Sample Data
-- Database Name: couponloop_db
-- =======================================================================

USE couponloop_db;

-- Clear previous sample data in reverse dependency order
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE admin_actions;
TRUNCATE TABLE coupon_reports;
TRUNCATE TABLE disputes;
TRUNCATE TABLE notifications;
TRUNCATE TABLE reviews;
TRUNCATE TABLE wishlist;
TRUNCATE TABLE seller_earnings;
TRUNCATE TABLE purchases;
TRUNCATE TABLE payments;
TRUNCATE TABLE coupons;
TRUNCATE TABLE coupon_categories;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. INSERT CATEGORIES
INSERT INTO coupon_categories (category_id, name, slug, description, icon_class, is_active) VALUES
(1, 'Food and Dining', 'food-dining', 'Discounts and vouchers for restaurants, food delivery, and dining outlets', 'fa-solid fa-utensils', 1),
(2, 'Shopping', 'shopping', 'Exclusive coupon codes for top e-commerce and retail stores', 'fa-solid fa-bag-shopping', 1),
(3, 'Travel and Cabs', 'travel-cabs', 'Savings on flight bookings, intercity cabs, trains, and bus rides', 'fa-solid fa-taxi', 1),
(4, 'Movies and Entertainment', 'movies-entertainment', 'BOGO and percentage discounts on movie tickets and theme parks', 'fa-solid fa-film', 1),
(5, 'Mobile Recharge', 'mobile-recharge', 'Cashback and recharge coupons for prepaid and postpaid mobile plans', 'fa-solid fa-mobile-screen', 1),
(6, 'OTT Subscriptions', 'ott-subscriptions', 'Discounts on annual and monthly streaming service passes', 'fa-solid fa-tv', 1),
(7, 'Hotels and Stays', 'hotels-stays', 'Discounted vouchers for premium hotels, resorts, and homestays', 'fa-solid fa-hotel', 1),
(8, 'Beauty and Fashion', 'beauty-fashion', 'Promo codes on luxury cosmetics, skincare, and apparel brands', 'fa-solid fa-shirt', 1),
(9, 'Groceries', 'groceries', 'Instant savings on quick-commerce, daily staples, and supermarket orders', 'fa-solid fa-basket-shopping', 1),
(10, 'Gift Vouchers', 'gift-vouchers', 'Transferable brand gift cards redeemable directly on merchant stores', 'fa-solid fa-gift', 1);

-- 2. INSERT USERS
-- Password for all sample users uses salt 'c0up0nl00psalt12'
-- Admin: Admin@123 -> e3efe7e0669f20ebe0810e3e79c3bfb4748dd15e58da7c58939e00225ef7ceb2
-- Seller: Seller@123 -> 942e005a915ea8098dcf58fe2360d03ea17e18758fec1679b6f7c94174e7aab8
-- Buyer: Buyer@123 -> 9e82fe8fd736cbfe1919fe3cae597b424c06358ba9a08f1353e3509f2bb074b7
-- User: User@123 -> 7ff0caf65e1c218322854eacdb7f3c2bf28e99dd045dc3b5986f9c16dd2153b7

INSERT INTO users (user_id, full_name, email, mobile, password_hash, salt, role, status, upi_id) VALUES
(1, 'Administrator', 'admin@couponloop.com', '9876543210', 'e3efe7e0669f20ebe0810e3e79c3bfb4748dd15e58da7c58939e00225ef7ceb2', 'c0up0nl00psalt12', 'ADMIN', 'ACTIVE', 'admin@upi'),
(2, 'Rahul Sharma', 'rahul.seller@gmail.com', '9845012345', '942e005a915ea8098dcf58fe2360d03ea17e18758fec1679b6f7c94174e7aab8', 'c0up0nl00psalt12', 'SELLER', 'ACTIVE', 'rahul@okaxis'),
(3, 'Priya Patel', 'priya.buyer@gmail.com', '9712345678', '9e82fe8fd736cbfe1919fe3cae597b424c06358ba9a08f1353e3509f2bb074b7', 'c0up0nl00psalt12', 'BUYER', 'ACTIVE', 'priya@okhdfcbank'),
(4, 'Anusha Kasani', 'anusha@couponloop.com', '9988776655', '7ff0caf65e1c218322854eacdb7f3c2bf28e99dd045dc3b5986f9c16dd2153b7', 'c0up0nl00psalt12', 'BUYER', 'ACTIVE', 'anusha@ybl');

-- =======================================================================
-- Note: All dummy/demonstration coupon listings have been removed.
-- Coupons must be uploaded by registered sellers and approved by administrators
-- to appear dynamically in the buyer marketplace.
-- =======================================================================
