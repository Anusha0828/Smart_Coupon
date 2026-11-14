/**
 * Global Application Framework for CouponLoop
 * Handles Navbar state, Session management, Toast notifications, Wishlist toggling, and Shared Modals.
 */

const App = (() => {
    let currentUser = null;

    // Toast Notification System
    function showToast(message, type = 'info') {
        let container = document.getElementById('toast-container-custom');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container-custom';
            container.className = 'toast-container-custom';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast-item ${type}`;

        let icon = 'fa-circle-info';
        if (type === 'success') icon = 'fa-circle-check';
        if (type === 'error') icon = 'fa-circle-exclamation';
        if (type === 'warning') icon = 'fa-triangle-exclamation';

        toast.innerHTML = `
            <i class="fa-solid ${icon} fs-5 text-${type === 'error' ? 'danger' : type}"></i>
            <div class="flex-grow-1">
                <div class="small fw-semibold text-dark">${message}</div>
            </div>
            <button type="button" class="btn-close btn-close-sm" style="font-size:0.7rem;"></button>
        `;

        const closeBtn = toast.querySelector('.btn-close');
        closeBtn.onclick = () => removeToast(toast);

        container.appendChild(toast);

        // Auto remove after 4.5 seconds
        setTimeout(() => removeToast(toast), 4500);
    }

    function removeToast(toast) {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        setTimeout(() => {
            if (toast.parentElement) toast.parentElement.removeChild(toast);
        }, 300);
    }

    // Currency Formatter
    function formatCurrency(amount) {
        return '₹' + Number(amount || 0).toLocaleString('en-IN', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 2
        });
    }

    // Copy to Clipboard Utility
    async function copyCode(code, btnElement) {
        try {
            await navigator.clipboard.writeText(code);
            showToast(`Coupon code "${code}" copied to clipboard!`, 'success');
            if (btnElement) {
                const originalHtml = btnElement.innerHTML;
                btnElement.innerHTML = '<i class="fa-solid fa-check me-1"></i> Copied!';
                btnElement.classList.add('btn-success');
                setTimeout(() => {
                    btnElement.innerHTML = originalHtml;
                    btnElement.classList.remove('btn-success');
                }, 2000);
            }
        } catch (e) {
            // Fallback for older browsers
            const textArea = document.createElement("textarea");
            textArea.value = code;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand("copy");
            document.body.removeChild(textArea);
            showToast(`Coupon code copied!`, 'success');
        }
    }

    // Wishlist Toggle
    async function toggleWishlist(couponId, buttonElement) {
        if (!currentUser) {
            showToast("Please sign in to save coupons to your wishlist.", "warning");
            setTimeout(() => {
                window.location.href = API.getBaseUrl() + "/login.html?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
            }, 1200);
            return;
        }

        try {
            const res = await API.wishlist.toggle(couponId);
            if (res.success) {
                const inWishlist = res.data && res.data.inWishlist;
                showToast(res.message, inWishlist ? "success" : "info");
                if (buttonElement) {
                    if (inWishlist) {
                        buttonElement.classList.add('active');
                        buttonElement.innerHTML = '<i class="fa-solid fa-heart text-danger"></i>';
                    } else {
                        buttonElement.classList.remove('active');
                        buttonElement.innerHTML = '<i class="fa-regular fa-heart"></i>';
                    }
                }
            }
        } catch (err) {
            showToast(err.message || "Failed to update wishlist", "error");
        }
    }

    // Check & Render Auth State in Navigation
    async function initNavigation() {
        const navAuthContainer = document.getElementById('nav-auth-container');
        if (!navAuthContainer) return;

        try {
            const res = await API.auth.currentUser();
            if (res.success && res.data && res.data.loggedIn) {
                currentUser = res.data;
                renderLoggedInNav(navAuthContainer, currentUser);
                checkUnreadNotifications();
            } else {
                currentUser = null;
                renderLoggedOutNav(navAuthContainer);
            }
        } catch (err) {
            currentUser = null;
            renderLoggedOutNav(navAuthContainer);
        }
    }

    function renderLoggedInNav(container, user) {
        const baseUrl = API.getBaseUrl();
        container.innerHTML = `
            <div class="d-flex align-items-center gap-2">
                <a href="${baseUrl}/wishlist.html" class="btn btn-sm btn-outline-secondary rounded-pill px-3 position-relative" title="My Wishlist">
                    <i class="fa-solid fa-heart text-danger"></i>
                </a>
                <div class="dropdown">
                    <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 position-relative" type="button" id="notifDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fa-solid fa-bell"></i>
                        <span id="notif-badge" class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle d-none">
                            <span class="visually-hidden">New alerts</span>
                        </span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow-lg p-2" id="notif-dropdown-list" style="width: 320px; max-height: 380px; overflow-y: auto;">
                        <li class="dropdown-header d-flex justify-content-between align-items-center">
                            <span class="fw-bold">Notifications</span>
                            <button class="btn btn-link btn-sm p-0 text-decoration-none" onclick="App.markAllNotificationsRead()">Mark all read</button>
                        </li>
                        <li><hr class="dropdown-divider"></li>
                        <div id="notif-items-container">
                            <li class="text-center py-3 text-muted small">Loading notifications...</li>
                        </div>
                    </ul>
                </div>
                <div class="dropdown">
                    <button class="btn btn-sm btn-primary rounded-pill px-3 dropdown-toggle d-flex align-items-center gap-2" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fa-solid fa-user-circle"></i>
                        <span class="d-none d-md-inline">${user.fullName.split(' ')[0]}</span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow-lg p-2 border-0 mt-2" style="border-radius: 12px; min-width: 220px;">
                        <li class="px-3 py-2 border-bottom">
                            <div class="fw-bold text-dark text-truncate">${user.fullName}</div>
                            <div class="small text-muted text-truncate">${user.email}</div>
                            <span class="badge bg-primary-light text-primary mt-1">${user.role}</span>
                        </li>
                        <li><a class="dropdown-item py-2 mt-1" href="${baseUrl}/buyer-dashboard.html"><i class="fa-solid fa-bag-shopping me-2 text-primary"></i>Buyer Dashboard</a></li>
                        <li><a class="dropdown-item py-2" href="${baseUrl}/seller-dashboard.html"><i class="fa-solid fa-store me-2 text-success"></i>Seller Dashboard</a></li>
                        <li><a class="dropdown-item py-2" href="${baseUrl}/upload-coupon.html"><i class="fa-solid fa-plus-circle me-2 text-warning"></i>Sell a Coupon</a></li>
                        <li><a class="dropdown-item py-2" href="${baseUrl}/wishlist.html"><i class="fa-solid fa-heart me-2 text-danger"></i>My Wishlist</a></li>
                        <li><a class="dropdown-item py-2" href="${baseUrl}/profile.html"><i class="fa-solid fa-gear me-2 text-secondary"></i>Account Settings</a></li>
                        ${user.isAdmin ? `<li><hr class="dropdown-divider"></li><li><a class="dropdown-item py-2 text-primary fw-bold" href="${baseUrl}/admin-dashboard.html"><i class="fa-solid fa-shield-halved me-2"></i>Admin Panel</a></li>` : ''}
                        <li><hr class="dropdown-divider"></li>
                        <li><button class="dropdown-item py-2 text-danger" onclick="App.logout()"><i class="fa-solid fa-arrow-right-from-bracket me-2"></i>Logout</button></li>
                    </ul>
                </div>
            </div>
        `;

        // Listen for notification dropdown open
        const notifDropdown = document.getElementById('notifDropdown');
        if (notifDropdown) {
            notifDropdown.addEventListener('show.bs.dropdown', loadNotificationItems);
        }
    }

    function renderLoggedOutNav(container) {
        const baseUrl = API.getBaseUrl();
        container.innerHTML = `
            <div class="d-flex align-items-center gap-2">
                <a href="${baseUrl}/login.html" class="btn btn-sm btn-outline-primary px-3">Sign In</a>
                <a href="${baseUrl}/register.html" class="btn btn-sm btn-primary px-3">Get Started</a>
            </div>
        `;
    }

    async function checkUnreadNotifications() {
        try {
            const res = await API.notifications.unreadCount();
            if (res.success && res.data && res.data.unreadCount > 0) {
                const badge = document.getElementById('notif-badge');
                if (badge) badge.classList.remove('d-none');
            }
        } catch (ignored) {}
    }

    async function loadNotificationItems() {
        const container = document.getElementById('notif-items-container');
        if (!container) return;

        try {
            const res = await API.notifications.list();
            if (res.success && res.data && res.data.length > 0) {
                container.innerHTML = res.data.map(n => `
                    <li class="p-2 border-bottom ${n.read ? 'bg-white' : 'bg-light'} rounded mb-1">
                        <div class="d-flex align-items-start gap-2">
                            <span class="badge ${n.type === 'SUCCESS' ? 'bg-success' : (n.type === 'EXPIRY' ? 'bg-warning text-dark' : 'bg-primary')} rounded-circle p-1 mt-1">
                                <i class="fa-solid ${n.type === 'EXPIRY' ? 'fa-clock' : 'fa-bell'}" style="font-size:0.65rem;"></i>
                            </span>
                            <div class="flex-grow-1">
                                <div class="fw-bold small text-dark">${n.title}</div>
                                <div class="text-muted" style="font-size:0.75rem;">${n.message}</div>
                                <div class="text-muted" style="font-size:0.65rem;">${n.createdAt || 'Recent'}</div>
                            </div>
                        </div>
                    </li>
                `).join('');
            } else {
                container.innerHTML = '<li class="text-center py-3 text-muted small">No new notifications.</li>';
            }
        } catch (e) {
            container.innerHTML = '<li class="text-center py-3 text-danger small">Failed to load notifications.</li>';
        }
    }

    async function markAllNotificationsRead() {
        try {
            await API.notifications.markAllRead();
            const badge = document.getElementById('notif-badge');
            if (badge) badge.classList.add('d-none');
            loadNotificationItems();
            showToast("Notifications marked as read.", "info");
        } catch (ignored) {}
    }

    async function logout() {
        try {
            await API.auth.logout();
            showToast("Logged out successfully.", "info");
            setTimeout(() => {
                window.location.href = API.getBaseUrl() + "/index.html";
            }, 800);
        } catch (err) {
            window.location.href = API.getBaseUrl() + "/index.html";
        }
    }

    // Modal Helpers
    let checkoutModalInstance = null;

    async function openCheckoutModal(couponId) {
        if (!currentUser) {
            showToast("Please sign in to purchase this voucher.", "warning");
            setTimeout(() => {
                window.location.href = API.getBaseUrl() + "/login.html?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
            }, 1200);
            return;
        }

        try {
            const previewRes = await API.purchase.preview(couponId);
            if (!previewRes.success) {
                showToast(previewRes.message, "error");
                return;
            }

            const p = previewRes.data;
            let modalEl = document.getElementById('checkoutSandboxModal');
            if (!modalEl) {
                modalEl = createCheckoutModalHtml();
                document.body.appendChild(modalEl);
            }

            // Populate checkout modal fields
            document.getElementById('checkout-coupon-title').innerText = p.title;
            document.getElementById('checkout-brand-name').innerText = p.brandName;
            document.getElementById('checkout-original-val').innerText = formatCurrency(p.originalValue);
            document.getElementById('checkout-selling-price').innerText = formatCurrency(p.sellingPrice);
            document.getElementById('checkout-savings').innerText = formatCurrency(p.savingsAmount);
            document.getElementById('checkout-conditions').innerText = p.usageConditions || "Standard brand terms apply.";
            document.getElementById('checkout-total-payable').innerText = formatCurrency(p.totalPayable);
            document.getElementById('checkout-coupon-id').value = p.couponId;

            // Reset result alert
            const alertBox = document.getElementById('checkout-alert-box');
            alertBox.classList.add('d-none');
            alertBox.innerHTML = '';
            document.getElementById('checkout-pay-btn').disabled = false;
            document.getElementById('checkout-pay-btn').innerHTML = `<i class="fa-solid fa-lock me-2"></i>Pay ${formatCurrency(p.totalPayable)} in Sandbox`;

            checkoutModalInstance = new bootstrap.Modal(modalEl);
            checkoutModalInstance.show();

        } catch (err) {
            showToast(err.message || "Unable to start checkout", "error");
        }
    }

    function createCheckoutModalHtml() {
        const div = document.createElement('div');
        div.className = 'modal fade';
        div.id = 'checkoutSandboxModal';
        div.tabIndex = -1;
        div.setAttribute('aria-hidden', 'true');
        div.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg" style="border-radius: 16px;">
                    <div class="modal-header border-0 pb-0">
                        <div>
                            <span class="badge bg-warning text-dark mb-1"><i class="fa-solid fa-flask me-1"></i> Sandbox Payment Gateway</span>
                            <h5 class="modal-title fw-bold">Secure Voucher Purchase</h5>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body pt-3">
                        <input type="hidden" id="checkout-coupon-id">
                        
                        <!-- Coupon Summary Card -->
                        <div class="p-3 bg-light rounded-3 mb-3 border">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <div>
                                    <span class="badge bg-primary-light text-primary fw-bold" id="checkout-brand-name">Brand</span>
                                    <h6 class="fw-bold mt-1 mb-0" id="checkout-coupon-title">Voucher Title</h6>
                                </div>
                                <div class="text-end">
                                    <span class="fs-5 fw-bold text-primary" id="checkout-selling-price">₹0</span>
                                    <div class="small text-muted"><del id="checkout-original-val">₹0</del></div>
                                </div>
                            </div>
                            <div class="d-flex justify-content-between small text-muted border-top pt-2">
                                <span>Your Savings: <strong class="text-success" id="checkout-savings">₹0</strong></span>
                                <span>Platform Fee: <strong>Included</strong></span>
                            </div>
                            <div class="small text-muted mt-2 border-top pt-2">
                                <strong>Terms:</strong> <span id="checkout-conditions">Conditions</span>
                            </div>
                        </div>

                        <!-- Sandbox Payment Method Selection -->
                        <label class="form-label fw-bold small text-muted mb-2">SELECT DEMO PAYMENT METHOD</label>
                        <div class="d-flex flex-column gap-2 mb-3">
                            <label class="d-flex align-items-center justify-content-between p-2.5 border rounded-3 cursor-pointer bg-white">
                                <div class="d-flex align-items-center gap-2">
                                    <input type="radio" name="sandboxMethod" value="UPI_DEMO" checked class="form-check-input mt-0">
                                    <i class="fa-solid fa-mobile-screen text-success fs-5"></i>
                                    <div>
                                        <div class="fw-semibold small">Demo UPI / QR</div>
                                        <div class="text-muted" style="font-size:0.75rem;">Instant simulated approval</div>
                                    </div>
                                </div>
                                <span class="badge bg-success-subtle text-success">Recommended</span>
                            </label>

                            <label class="d-flex align-items-center justify-content-between p-2.5 border rounded-3 cursor-pointer bg-white">
                                <div class="d-flex align-items-center gap-2">
                                    <input type="radio" name="sandboxMethod" value="CARD_DEMO" class="form-check-input mt-0">
                                    <i class="fa-regular fa-credit-card text-primary fs-5"></i>
                                    <div>
                                        <div class="fw-semibold small">Demo Credit / Debit Card</div>
                                        <div class="text-muted" style="font-size:0.75rem;">Simulated 3D Secure OTP verification</div>
                                    </div>
                                </div>
                            </label>

                            <label class="d-flex align-items-center justify-content-between p-2.5 border rounded-3 cursor-pointer bg-white">
                                <div class="d-flex align-items-center gap-2">
                                    <input type="radio" name="sandboxMethod" value="NETBANKING_DEMO" class="form-check-input mt-0">
                                    <i class="fa-solid fa-building-columns text-info fs-5"></i>
                                    <div>
                                        <div class="fw-semibold small">Demo NetBanking</div>
                                        <div class="text-muted" style="font-size:0.75rem;">Simulated corporate/retail banking</div>
                                    </div>
                                </div>
                            </label>
                        </div>

                        <!-- Test Simulation Trigger -->
                        <div class="p-2 bg-secondary-light border rounded-3 mb-3 small d-flex justify-content-between align-items-center">
                            <span>Gateway Simulation Mode:</span>
                            <select id="checkout-simulation-mode" class="form-select form-select-sm w-auto">
                                <option value="SUCCESS" selected>Simulate Success</option>
                                <option value="FAILURE">Simulate Bank Failure</option>
                            </select>
                        </div>

                        <!-- Alert Box -->
                        <div id="checkout-alert-box" class="alert d-none small mb-0"></div>
                    </div>
                    <div class="modal-footer border-0 pt-0">
                        <div class="w-100 d-flex justify-content-between align-items-center mb-2">
                            <span class="text-muted small">Total Amount:</span>
                            <span class="fs-4 fw-bolder text-dark" id="checkout-total-payable">₹0</span>
                        </div>
                        <button type="button" class="btn btn-secondary flex-grow-1" data-bs-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-primary flex-grow-1" id="checkout-pay-btn" onclick="App.processSandboxPayment()">
                            <i class="fa-solid fa-lock me-2"></i>Pay in Sandbox
                        </button>
                    </div>
                </div>
            </div>
        `;
        return div;
    }

    async function processSandboxPayment() {
        const couponId = document.getElementById('checkout-coupon-id').value;
        const methodRadios = document.getElementsByName('sandboxMethod');
        let paymentMethod = 'UPI_DEMO';
        for (const r of methodRadios) {
            if (r.checked) {
                paymentMethod = r.value;
                break;
            }
        }

        const simMode = document.getElementById('checkout-simulation-mode').value;
        const payBtn = document.getElementById('checkout-pay-btn');
        const alertBox = document.getElementById('checkout-alert-box');

        payBtn.disabled = true;
        payBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Processing with Sandbox...';
        alertBox.classList.add('d-none');

        try {
            const res = await API.purchase.pay({
                couponId: Number(couponId),
                paymentMethod: paymentMethod,
                simulateResult: simMode
            });

            if (res.success) {
                const p = res.data;
                alertBox.className = 'alert alert-success small mb-0 mt-2';
                alertBox.innerHTML = `
                    <div class="fw-bold mb-1"><i class="fa-solid fa-circle-check me-1"></i> Payment Verified!</div>
                    <div>Your code: <strong class="fs-6 text-primary">${p.couponCode}</strong></div>
                    <div class="mt-2">
                        <button class="btn btn-sm btn-outline-primary" onclick="App.copyCode('${p.couponCode}', this)">Copy Code</button>
                        <a href="${API.getBaseUrl()}/buyer-dashboard.html" class="btn btn-sm btn-primary ms-1">Go to My Purchases</a>
                    </div>
                `;
                alertBox.classList.remove('d-none');
                payBtn.classList.add('d-none');
                showToast("Voucher unlocked successfully!", "success");
            } else {
                alertBox.className = 'alert alert-danger small mb-0 mt-2';
                alertBox.innerHTML = `<i class="fa-solid fa-triangle-exclamation me-1"></i> ${res.message}`;
                alertBox.classList.remove('d-none');
                payBtn.disabled = false;
                payBtn.innerHTML = 'Try Again';
            }
        } catch (err) {
            alertBox.className = 'alert alert-danger small mb-0 mt-2';
            alertBox.innerHTML = `<i class="fa-solid fa-triangle-exclamation me-1"></i> ${err.message || 'Payment simulation failed.'}`;
            alertBox.classList.remove('d-none');
            payBtn.disabled = false;
            payBtn.innerHTML = 'Try Again';
        }
    }

    // Eligibility Checker Modal
    function openEligibilityModal() {
        let modalEl = document.getElementById('eligibilityModal');
        if (!modalEl) {
            modalEl = document.createElement('div');
            modalEl.className = 'modal fade';
            modalEl.id = 'eligibilityModal';
            modalEl.tabIndex = -1;
            modalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg" style="border-radius: 16px;">
                        <div class="modal-header border-0 pb-0">
                            <h5 class="modal-title fw-bold"><i class="fa-solid fa-shield-check text-primary me-2"></i>Coupon Transferability Checker</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <p class="small text-muted">CouponLoop protects buyers by strictly enforcing merchant voucher transferability guidelines. Only transferable vouchers may be listed.</p>
                            
                            <div class="list-group list-group-flush small mb-3">
                                <div class="list-group-item d-flex align-items-center gap-2">
                                    <i class="fa-solid fa-check text-success fs-5"></i>
                                    <div><strong>Permitted:</strong> Universal promotional promo codes and gift voucher codes redeemable on merchant apps without account locking.</div>
                                </div>
                                <div class="list-group-item d-flex align-items-center gap-2">
                                    <i class="fa-solid fa-xmark text-danger fs-5"></i>
                                    <div><strong>Prohibited:</strong> Direct UPI cashback scratch cards bound to your personal bank account or registered mobile number.</div>
                                </div>
                                <div class="list-group-item d-flex align-items-center gap-2">
                                    <i class="fa-solid fa-xmark text-danger fs-5"></i>
                                    <div><strong>Prohibited:</strong> Expired vouchers or codes with restrictive Single-Device/Account-bound OTP locks.</div>
                                </div>
                            </div>

                            <div class="p-3 bg-light rounded border text-muted small">
                                <strong>Legal Notice:</strong> CouponLoop operates as a secondary exchange for eligible unused vouchers. Sellers bear responsibility for verifying merchant transfer rights.
                            </div>
                        </div>
                        <div class="modal-footer border-0 pt-0">
                            <button type="button" class="btn btn-primary w-100" data-bs-dismiss="modal">I Understand Platform Rules</button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(modalEl);
        }
        new bootstrap.Modal(modalEl).show();
    }

    // Initialize Global Listeners on Page Load
    document.addEventListener('DOMContentLoaded', () => {
        initNavigation();
    });

    return {
        showToast,
        copyCode,
        toggleWishlist,
        openCheckoutModal,
        openEligibilityModal,
        processSandboxPayment,
        markAllNotificationsRead,
        formatCurrency,
        logout,
        getCurrentUser: () => currentUser
    };
})();
