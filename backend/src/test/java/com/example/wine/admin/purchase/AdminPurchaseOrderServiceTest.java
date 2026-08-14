package com.example.wine.admin.purchase;

import com.example.wine.model.PurchaseOrder;
import com.example.wine.model.Wine;
import com.example.wine.repository.PurchaseOrderRepository;
import com.example.wine.repository.WineRepository;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private WineRepository wineRepository;
    @Mock
    private ApplicationTelemetry telemetry;

    private AdminPurchaseOrderService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-14T03:00:00Z"), ZoneOffset.UTC);
        service = new AdminPurchaseOrderService(purchaseOrderRepository, wineRepository, telemetry, clock);
    }

    @Test
    void createsOrderWithDeliveryFiveBusinessDaysLater() {
        Wine wine = wine(3L, 35);
        when(wineRepository.findById(3L)).thenReturn(Optional.of(wine));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminPurchaseOrderSummary result = service.createPurchaseOrder(3L, 12);

        assertThat(result.getOrderedDate()).isEqualTo("2026-08-14");
        assertThat(result.getDeliveryDate()).isEqualTo("2026-08-21");
        assertThat(result.getQuantity()).isEqualTo(12);
    }

    @Test
    void receivingOrderAddsStockAndDeletesOrder() {
        Wine wine = wine(3L, 35);
        PurchaseOrder order = new PurchaseOrder();
        order.setId(7L);
        order.setWine(wine);
        order.setQuantity(12);
        when(purchaseOrderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(order));
        when(wineRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(wine));

        service.receivePurchaseOrder(7L);

        ArgumentCaptor<Wine> savedWine = ArgumentCaptor.forClass(Wine.class);
        verify(wineRepository).save(savedWine.capture());
        assertThat(savedWine.getValue().getStock()).isEqualTo(47);
        verify(purchaseOrderRepository).delete(order);
        verify(telemetry).adminPurchaseOrderReceived(7L, 3L, 12, 35, 47);
    }

    private Wine wine(Long id, int stock) {
        Wine wine = new Wine();
        wine.setId(id);
        wine.setName("Aotearoa Cellars");
        wine.setStock(stock);
        return wine;
    }
}