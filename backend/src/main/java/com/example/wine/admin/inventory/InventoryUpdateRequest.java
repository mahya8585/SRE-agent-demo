package com.example.wine.admin.inventory;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class InventoryUpdateRequest {
    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    @Min(0)
    private Integer threshold;

    @Size(max = 2000)
    private String description;

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}