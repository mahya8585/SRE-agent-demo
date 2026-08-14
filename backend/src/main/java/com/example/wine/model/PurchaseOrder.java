package com.example.wine.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wine_id", nullable = false)
    private Wine wine;

    private Integer quantity;
    private LocalDate orderedDate;
    private LocalDate deliveryDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Wine getWine() { return wine; }
    public void setWine(Wine wine) { this.wine = wine; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDate getOrderedDate() { return orderedDate; }
    public void setOrderedDate(LocalDate orderedDate) { this.orderedDate = orderedDate; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
}