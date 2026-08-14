package com.example.wine.admin.dto;

public class AdminDashboardSummary {
    private final long orderCount;
    private final double revenue;
    private final long lowStockCount;
    private final long totalStock;

    public AdminDashboardSummary(long orderCount, double revenue, long lowStockCount, long totalStock) {
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.lowStockCount = lowStockCount;
        this.totalStock = totalStock;
    }

    public long getOrderCount() { return orderCount; }
    public double getRevenue() { return revenue; }
    public long getLowStockCount() { return lowStockCount; }
    public long getTotalStock() { return totalStock; }
}