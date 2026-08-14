package com.example.wine.admin.inventory;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class InventoryUpdateRequest {
    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    @Min(0)
    private Integer threshold;

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }
}