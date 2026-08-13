package com.example.wine.controller;

import com.example.wine.model.CheckoutRequest;
import com.example.wine.model.CustomerOrder;
import com.example.wine.model.OrderConfirmation;
import com.example.wine.model.OrderItemRequest;
import com.example.wine.model.OrderLine;
import com.example.wine.model.Wine;
import com.example.wine.repository.CustomerOrderRepository;
import com.example.wine.repository.OrderLineRepository;
import com.example.wine.repository.WineRepository;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final double FREE_SHIPPING_THRESHOLD = 15000.0;
    private static final double SHIPPING_FEE = 800.0;

    private final WineRepository wineRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderLineRepository orderLineRepository;
    private final ApplicationTelemetry telemetry;

    public OrderController(WineRepository wineRepository, CustomerOrderRepository customerOrderRepository,
                           OrderLineRepository orderLineRepository, ApplicationTelemetry telemetry) {
        this.wineRepository = wineRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderLineRepository = orderLineRepository;
        this.telemetry = telemetry;
    }

    @PostMapping
    @Transactional
    public OrderConfirmation create(@Valid @RequestBody CheckoutRequest request) {
        List<Wine> purchasedWines = new ArrayList<>();
        double subtotal = 0.0;
        int itemCount = 0;

        for (OrderItemRequest item : request.getItems()) {
            Wine wine = wineRepository.findByIdForUpdate(item.getWineId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wine not found"));

            if (wine.getStock() < item.getQuantity()) {
                telemetry.stockRejected(wine.getId(), item.getQuantity(), wine.getStock());
                throw new ResponseStatusException(HttpStatus.CONFLICT, wine.getName() + " is out of stock");
            }

            wine.setStock(wine.getStock() - item.getQuantity());
            purchasedWines.add(wine);
            subtotal += wine.getPrice() * item.getQuantity();
            itemCount += item.getQuantity();
        }

        wineRepository.saveAll(purchasedWines);
        double shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0.0 : SHIPPING_FEE;
        String orderNumber = "MV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(orderNumber);
        order.setStatus("CONFIRMED");
        order.setCustomerName(request.getCustomerName());
        order.setEmail(request.getEmail());
        order.setDeliveryAddress(request.getAddress());
        order.setItemCount(itemCount);
        order.setSubtotal(subtotal);
        order.setShipping(shipping);
        order.setTotal(subtotal + shipping);
        order.setCreatedAt(OffsetDateTime.now());
        customerOrderRepository.save(order);

        List<OrderLine> orderLines = new ArrayList<>();
        for (int index = 0; index < request.getItems().size(); index++) {
            OrderItemRequest item = request.getItems().get(index);
            Wine wine = purchasedWines.get(index);
            OrderLine line = new OrderLine();
            line.setOrderId(order.getId());
            line.setWineId(wine.getId());
            line.setWineName(wine.getName());
            line.setUnitPrice(wine.getPrice());
            line.setQuantity(item.getQuantity());
            line.setLineTotal(wine.getPrice() * item.getQuantity());
            orderLines.add(line);
        }
        orderLineRepository.saveAll(orderLines);
        telemetry.orderConfirmed(orderNumber, itemCount, subtotal + shipping, shipping == 0.0);

        return new OrderConfirmation(
            orderNumber,
                "CONFIRMED",
                itemCount,
                subtotal,
                shipping,
                subtotal + shipping
        );
    }
}