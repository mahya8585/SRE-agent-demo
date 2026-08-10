package com.example.wine.model;

public class OrderConfirmation {
    private final String orderId;
    private final String status;
    private final int itemCount;
    private final double subtotal;
    private final double shipping;
    private final double total;

    public OrderConfirmation(String orderId, String status, int itemCount, double subtotal, double shipping, double total) {
        this.orderId = orderId;
        this.status = status;
        this.itemCount = itemCount;
        this.subtotal = subtotal;
        this.shipping = shipping;
        this.total = total;
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public int getItemCount() { return itemCount; }
    public double getSubtotal() { return subtotal; }
    public double getShipping() { return shipping; }
    public double getTotal() { return total; }
}