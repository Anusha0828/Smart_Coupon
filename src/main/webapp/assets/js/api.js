/**
 * Centralized API Client for CouponLoop REST Servlets
 */
const API = (() => {
    // Dynamically resolve context path
    const getContextPath = () => {
        const path = window.location.pathname;
        const segments = path.split('/');
        // If hosted at /couponloop/... or subpath
        if (segments.length > 2 && segments[1] && !segments[1].includes('.')) {
            return '/' + segments[1];
        }
        return '';
    };

    const BASE_URL = getContextPath();

    async function request(endpoint, options = {}) {
        const url = BASE_URL + endpoint;
        const defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };

        const config = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, config);
            const data = await response.json().catch(() => null);

            if (!response.ok) {
                const errorMessage = (data && data.message) ? data.message : `HTTP error ${response.status}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (err) {
            console.error(`API Error on ${endpoint}:`, err);
            throw err;
        }
    }

    return {
        getBaseUrl: () => BASE_URL,
        get: (endpoint) => request(endpoint, { method: 'GET' }),
        post: (endpoint, body) => request(endpoint, { method: 'POST', body: JSON.stringify(body) }),
        
        // Auth API
        auth: {
            currentUser: () => request('/api/auth/current-user', { method: 'GET' }),
            login: (email, password) => request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
            register: (userData) => request('/api/auth/register', { method: 'POST', body: JSON.stringify(userData) }),
            logout: () => request('/api/auth/logout', { method: 'POST' }),
            forgotPassword: (email) => request('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
            resetPassword: (payload) => request('/api/auth/reset-password', { method: 'POST', body: JSON.stringify(payload) })
        },

        // Coupon API
        coupons: {
            search: (params = {}) => {
                const query = new URLSearchParams(params).toString();
                return request('/api/coupons' + (query ? '?' + query : ''), { method: 'GET' });
            },
            details: (id) => request(`/api/coupons/details?id=${id}`, { method: 'GET' }),
            recommendations: (limit = 8) => request(`/api/coupons/recommendations?limit=${limit}`, { method: 'GET' }),
            categories: () => request('/api/coupons/categories', { method: 'GET' }),
            brands: () => request('/api/coupons/brands', { method: 'GET' }),
            smartPricing: (payload) => request('/api/coupons/smart-pricing', { method: 'POST', body: JSON.stringify(payload) }),
            eligibilityCheck: (payload) => request('/api/coupons/eligibility-check', { method: 'POST', body: JSON.stringify(payload) })
        },

        // Buyer API
        buyer: {
            dashboard: () => request('/api/buyer/dashboard', { method: 'GET' }),
            purchases: () => request('/api/buyer/purchases', { method: 'GET' }),
            purchaseDetails: (id) => request(`/api/buyer/purchase-details?id=${id}`, { method: 'GET' })
        },

        // Seller API
        seller: {
            dashboard: () => request('/api/seller/dashboard', { method: 'GET' }),
            myCoupons: () => request('/api/seller/my-coupons', { method: 'GET' }),
            upload: (couponData) => request('/api/seller/upload', { method: 'POST', body: JSON.stringify(couponData) }),
            update: (couponData) => request('/api/seller/update', { method: 'POST', body: JSON.stringify(couponData) }),
            delete: (couponId) => request('/api/seller/delete', { method: 'POST', body: JSON.stringify({ couponId }) }),
            earnings: () => request('/api/seller/earnings', { method: 'GET' }),
            requestPayout: (upiId) => request('/api/seller/request-payout', { method: 'POST', body: JSON.stringify({ upiId }) })
        },

        // Purchase & Sandbox Payment API
        purchase: {
            preview: (couponId) => request(`/api/purchase/preview?couponId=${couponId}`, { method: 'GET' }),
            pay: (payload) => request('/api/purchase/pay', { method: 'POST', body: JSON.stringify(payload) })
        },

        // Wishlist API
        wishlist: {
            list: () => request('/api/wishlist/list', { method: 'GET' }),
            toggle: (couponId) => request('/api/wishlist/toggle', { method: 'POST', body: JSON.stringify({ couponId }) })
        },

        // Reviews API
        reviews: {
            submit: (payload) => request('/api/reviews/submit', { method: 'POST', body: JSON.stringify(payload) }),
            getForSeller: (sellerId) => request(`/api/reviews/seller?sellerId=${sellerId}`, { method: 'GET' })
        },

        // Disputes API
        disputes: {
            raise: (payload) => request('/api/disputes/raise', { method: 'POST', body: JSON.stringify(payload) }),
            reportCoupon: (payload) => request('/api/disputes/report-coupon', { method: 'POST', body: JSON.stringify(payload) }),
            myDisputes: () => request('/api/disputes/my-disputes', { method: 'GET' })
        },

        // Notifications API
        notifications: {
            list: () => request('/api/notifications', { method: 'GET' }),
            unreadCount: () => request('/api/notifications/unread-count', { method: 'GET' }),
            markRead: (notificationId) => request('/api/notifications/mark-read', { method: 'POST', body: JSON.stringify({ notificationId }) }),
            markAllRead: () => request('/api/notifications/mark-all-read', { method: 'POST' })
        },

        // Admin API
        admin: {
            overview: () => request('/api/admin/overview', { method: 'GET' }),
            users: () => request('/api/admin/users', { method: 'GET' }),
            toggleUserStatus: (userId, status) => request('/api/admin/toggle-user-status', { method: 'POST', body: JSON.stringify({ userId, status }) }),
            coupons: (status = 'ALL') => request(`/api/admin/coupons?status=${status}`, { method: 'GET' }),
            approveCoupon: (couponId) => request('/api/admin/approve-coupon', { method: 'POST', body: JSON.stringify({ couponId }) }),
            rejectCoupon: (couponId, rejectionReason) => request('/api/admin/reject-coupon', { method: 'POST', body: JSON.stringify({ couponId, rejectionReason }) }),
            updateCouponStatus: (couponId, status) => request('/api/admin/update-coupon-status', { method: 'POST', body: JSON.stringify({ couponId, status }) }),
            transactions: () => request('/api/admin/transactions', { method: 'GET' }),
            disputes: () => request('/api/admin/disputes', { method: 'GET' }),
            resolveDispute: (payload) => request('/api/admin/resolve-dispute', { method: 'POST', body: JSON.stringify(payload) }),
            analytics: () => request('/api/admin/analytics', { method: 'GET' })
        },

        // User profile API
        user: {
            profile: () => request('/api/user/profile', { method: 'GET' }),
            updateProfile: (data) => request('/api/user/update-profile', { method: 'POST', body: JSON.stringify(data) }),
            updateUpi: (upiId) => request('/api/user/update-upi', { method: 'POST', body: JSON.stringify({ upiId }) }),
            changePassword: (data) => request('/api/user/change-password', { method: 'POST', body: JSON.stringify(data) })
        }
    };
})();
