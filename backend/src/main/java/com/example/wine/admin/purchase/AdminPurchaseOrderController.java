package com.example.wine.admin.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/purchase-orders")
public class AdminPurchaseOrderController {
    private final AdminPurchaseOrderService purchaseOrderService;

    public AdminPurchaseOrderController(AdminPurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    public List<AdminPurchaseOrderSummary> listPurchaseOrders() {
        return purchaseOrderService.listPurchaseOrders();
    }

    @PostMapping
    public ResponseEntity<AdminPurchaseOrderSummary> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseOrderService.createPurchaseOrder(request.getWineId(), request.getQuantity()));
    }

    @PostMapping("/{id}/receive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receivePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.receivePurchaseOrder(id);
    }
}