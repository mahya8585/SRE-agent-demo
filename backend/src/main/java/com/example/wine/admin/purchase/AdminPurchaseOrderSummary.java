package com.example.wine.admin.purchase;

import com.example.wine.model.PurchaseOrder;

import java.time.LocalDate;

public class AdminPurchaseOrderSummary {
    private final Long id;
    private final Long wineId;
    private final String wineName;
    private final Integer quantity;
    private final LocalDate orderedDate;
    private final LocalDate deliveryDate;

    public AdminPurchaseOrderSummary(Long id, Long wineId, String wineName, Integer quantity,
                                     LocalDate orderedDate, LocalDate deliveryDate) {
        this.id = id;
        this.wineId = wineId;
        this.wineName = wineName;
        this.quantity = quantity;
        this.orderedDate = orderedDate;
        this.deliveryDate = deliveryDate;
    }

    public static AdminPurchaseOrderSummary from(PurchaseOrder order) {
        return new AdminPurchaseOrderSummary(order.getId(), order.getWine().getId(), order.getWine().getName(),
                order.getQuantity(), order.getOrderedDate(), order.getDeliveryDate());
    }

    public Long getId() { return id; }
    public Long getWineId() { return wineId; }
    public String getWineName() { return wineName; }
    public Integer getQuantity() { return quantity; }
    public LocalDate getOrderedDate() { return orderedDate; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
}