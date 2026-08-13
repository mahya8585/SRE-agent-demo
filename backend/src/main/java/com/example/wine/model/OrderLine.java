package com.example.wine.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long wineId;
    private String wineName;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;

    public void setOrderId(Long value) { orderId = value; }
    public void setWineId(Long value) { wineId = value; }
    public void setWineName(String value) { wineName = value; }
    public void setUnitPrice(Double value) { unitPrice = value; }
    public void setQuantity(Integer value) { quantity = value; }
    public void setLineTotal(Double value) { lineTotal = value; }
}