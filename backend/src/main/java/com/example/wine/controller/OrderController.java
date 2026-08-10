package com.example.wine.controller;

import com.example.wine.model.CheckoutRequest;
import com.example.wine.model.OrderConfirmation;
import com.example.wine.model.OrderItemRequest;
import com.example.wine.model.Wine;
import com.example.wine.repository.WineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final double FREE_SHIPPING_THRESHOLD = 15000.0;
    private static final double SHIPPING_FEE = 800.0;

    private final WineRepository wineRepository;

    public OrderController(WineRepository wineRepository) {
        this.wineRepository = wineRepository;
    }

    @PostMapping
    @Transactional
    public OrderConfirmation create(@Valid @RequestBody CheckoutRequest request) {
        List<Wine> purchasedWines = new ArrayList<>();
        double subtotal = 0.0;
        int itemCount = 0;

        for (OrderItemRequest item : request.getItems()) {
            Wine wine = wineRepository.findById(item.getWineId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wine not found"));

            if (wine.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, wine.getName() + " is out of stock");
            }

            wine.setStock(wine.getStock() - item.getQuantity());
            purchasedWines.add(wine);
            subtotal += wine.getPrice() * item.getQuantity();
            itemCount += item.getQuantity();
        }

        wineRepository.saveAll(purchasedWines);
        double shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0.0 : SHIPPING_FEE;

        return new OrderConfirmation(
                "MV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "CONFIRMED",
                itemCount,
                subtotal,
                shipping,
                subtotal + shipping
        );
    }
}