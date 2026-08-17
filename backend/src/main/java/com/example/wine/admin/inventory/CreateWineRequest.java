package com.example.wine.admin.inventory;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateWineRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 64)
    private String category;

    @Size(max = 255)
    private String region;

    @Size(max = 255)
    private String variety;

    @Size(max = 32)
    private String vintage;

    @Size(max = 512)
    private String image;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin("0.0")
    private Double price;

    @NotNull
    @Min(0)
    private Integer stock;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public String getVintage() { return vintage; }
    public void setVintage(String vintage) { this.vintage = vintage; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}