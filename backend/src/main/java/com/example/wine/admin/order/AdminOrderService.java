package com.example.wine.admin.order;

import com.example.wine.model.CustomerOrder;
import com.example.wine.repository.CustomerOrderRepository;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(
            Arrays.asList("CONFIRMED", "PROCESSING", "SHIPPED"));

    private final CustomerOrderRepository orderRepository;
    private final ApplicationTelemetry telemetry;

    public AdminOrderService(CustomerOrderRepository orderRepository, ApplicationTelemetry telemetry) {
        this.orderRepository = orderRepository;
        this.telemetry = telemetry;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderSummary> listOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminOrderSummary::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminOrderSummary updateStatus(Long id, String requestedStatus) {
        String status = requestedStatus.toUpperCase();
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported order status");
        }

        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        String previousStatus = order.getStatus();
        order.setStatus(status);
        CustomerOrder saved = orderRepository.save(order);
        telemetry.adminOrderStatusChanged(saved.getId(), previousStatus, status);
        return AdminOrderSummary.from(saved);
    }
}