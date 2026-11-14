/**
 * Chart.js Integration for CouponLoop Analytics
 */

const ChartAnalytics = (() => {
    let categoryChart = null;
    let revenueChart = null;
    let sellerChart = null;

    function renderCategoryDistribution(canvasId, data = []) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (categoryChart) categoryChart.destroy();

        const labels = data.map(d => d.category);
        const counts = data.map(d => d.count);
        const palette = [
            '#6366f1', '#2563eb', '#8b5cf6', '#06b6d4', '#10b981',
            '#f59e0b', '#ec4899', '#3b82f6', '#14b8a6', '#f97316'
        ];

        categoryChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels.length ? labels : ['Food', 'Shopping', 'Travel', 'Entertainment'],
                datasets: [{
                    data: counts.length ? counts : [4, 3, 2, 2],
                    backgroundColor: palette,
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { boxWidth: 12, font: { family: 'Plus Jakarta Sans', size: 11 } }
                    }
                },
                cutout: '65%'
            }
        });
    }

    function renderMonthlyVolume(canvasId) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (revenueChart) revenueChart.destroy();

        revenueChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov'],
                datasets: [
                    {
                        label: 'Gross Volume (₹)',
                        data: [1200, 2400, 4800, 6200, 9500, 14200],
                        backgroundColor: '#6366f1',
                        borderRadius: 6
                    },
                    {
                        label: 'Commission Earned (₹)',
                        data: [120, 240, 480, 620, 950, 1420],
                        backgroundColor: '#2563eb',
                        borderRadius: 6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: { boxWidth: 12, font: { family: 'Plus Jakarta Sans', size: 12 } }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: '#f1f5f9' },
                        ticks: { font: { family: 'Plus Jakarta Sans', size: 11 } }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { font: { family: 'Plus Jakarta Sans', size: 11 } }
                    }
                }
            }
        });
    }

    function renderSellerPerformance(canvasId, activeCount, soldCount, expiredCount) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (sellerChart) sellerChart.destroy();

        sellerChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Active Listings', 'Coupons Sold', 'Expired'],
                datasets: [{
                    data: [activeCount || 0, soldCount || 0, expiredCount || 0],
                    backgroundColor: ['#10b981', '#6366f1', '#ef4444'],
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { boxWidth: 12, font: { family: 'Plus Jakarta Sans', size: 11 } }
                    }
                },
                cutout: '70%'
            }
        });
    }

    return {
        renderCategoryDistribution,
        renderMonthlyVolume,
        renderSellerPerformance
    };
})();
