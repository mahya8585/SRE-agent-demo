package com.example.wine.admin.order;

import com.example.wine.model.CustomerOrder;

import java.time.OffsetDateTime;

public class AdminOrderSummary {
    private final Long id;
    private final String orderNumber;
    private final String status;
    private final String customerName;
    private final Integer itemCount;
    private final Double total;
    private final OffsetDateTime createdAt;

    public AdminOrderSummary(Long id, String orderNumber, String status, String customerName,
                             Integer itemCount, Double total, OffsetDateTime createdAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.customerName = customerName;
        this.itemCount = itemCount;
        this.total = total;
        this.createdAt = createdAt;
    }

    public static AdminOrderSummary from(CustomerOrder order) {
        return new AdminOrderSummary(order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getCustomerName(), order.getItemCount(), order.getTotal(), order.getCreatedAt());
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public Integer getItemCount() { return itemCount; }
    public Double getTotal() { return total; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}