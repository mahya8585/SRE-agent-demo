package com.example.wine.admin.purchase;

import com.example.wine.model.PurchaseOrder;
import com.example.wine.model.Wine;
import com.example.wine.repository.PurchaseOrderRepository;
import com.example.wine.repository.WineRepository;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminPurchaseOrderService {
    private static final int DELIVERY_BUSINESS_DAYS = 5;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WineRepository wineRepository;
    private final ApplicationTelemetry telemetry;
    private final Clock clock;

    public AdminPurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                     WineRepository wineRepository,
                                     ApplicationTelemetry telemetry,
                                     Clock clock) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.wineRepository = wineRepository;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminPurchaseOrderSummary> listPurchaseOrders() {
        return purchaseOrderRepository.findAllWithWineOrderByDeliveryDate().stream()
                .map(AdminPurchaseOrderSummary::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminPurchaseOrderSummary createPurchaseOrder(Long wineId, int quantity) {
        Wine wine = wineRepository.findById(wineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wine not found"));
        LocalDate orderedDate = LocalDate.now(clock);

        PurchaseOrder order = new PurchaseOrder();
        order.setWine(wine);
        order.setQuantity(quantity);
        order.setOrderedDate(orderedDate);
        order.setDeliveryDate(BusinessDayCalculator.addBusinessDays(orderedDate, DELIVERY_BUSINESS_DAYS));
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        telemetry.adminPurchaseOrderCreated(saved.getId(), wineId, quantity, saved.getDeliveryDate());
        return AdminPurchaseOrderSummary.from(saved);
    }

    @Transactional
    public void receivePurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));
        Long wineId = order.getWine().getId();
        Wine wine = wineRepository.findByIdForUpdate(wineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wine not found"));
        int previousStock = wine.getStock();
        int quantity = order.getQuantity();
        wine.setStock(previousStock + quantity);
        wineRepository.save(wine);
        purchaseOrderRepository.delete(order);
        telemetry.adminPurchaseOrderReceived(id, wineId, quantity, previousStock, wine.getStock());
    }
}