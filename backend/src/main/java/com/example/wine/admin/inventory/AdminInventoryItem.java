package com.example.wine.admin.inventory;

import com.example.wine.model.Wine;

public class AdminInventoryItem {
    private final Long id;
    private final String name;
    private final String category;
    private final Integer stock;
    private final Integer threshold;
    private final Double price;

    public AdminInventoryItem(Long id, String name, String category, Integer stock, Integer threshold, Double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.stock = stock;
        this.threshold = threshold;
        this.price = price;
    }

    public static AdminInventoryItem from(Wine wine) {
        return new AdminInventoryItem(wine.getId(), wine.getName(), wine.getCategory(),
                wine.getStock(), wine.getThreshold(), wine.getPrice());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Integer getStock() { return stock; }
    public Integer getThreshold() { return threshold; }
    public Double getPrice() { return price; }
}