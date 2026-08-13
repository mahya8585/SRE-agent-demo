package com.example.wine.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNumber;
    private String status;
    private String customerName;
    private String email;
    private String deliveryAddress;
    private Integer itemCount;
    private Double subtotal;
    private Double shipping;
    private Double total;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String value) { orderNumber = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { customerName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String value) { deliveryAddress = value; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer value) { itemCount = value; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double value) { subtotal = value; }
    public Double getShipping() { return shipping; }
    public void setShipping(Double value) { shipping = value; }
    public Double getTotal() { return total; }
    public void setTotal(Double value) { total = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}