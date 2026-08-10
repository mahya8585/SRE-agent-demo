package com.example.wine.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class OrderItemRequest {
    @NotNull
    private Long wineId;

    @Min(1)
    private int quantity;

    public Long getWineId() { return wineId; }
    public void setWineId(Long wineId) { this.wineId = wineId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}