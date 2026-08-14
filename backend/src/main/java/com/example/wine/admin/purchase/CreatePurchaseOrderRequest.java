package com.example.wine.admin.purchase;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class CreatePurchaseOrderRequest {
    @NotNull
    private Long wineId;

    @NotNull
    @Positive
    private Integer quantity;

    public Long getWineId() { return wineId; }
    public void setWineId(Long wineId) { this.wineId = wineId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}