package com.example.wine.admin.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<AdminOrderSummary> listOrders() {
        return orderService.listOrders();
    }

    @PutMapping("/{id}/status")
    public AdminOrderSummary updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }
}