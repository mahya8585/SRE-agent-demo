package com.example.wine.admin.purchase;

import com.example.wine.telemetry.ApplicationTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPurchaseOrderController.class)
class AdminPurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminPurchaseOrderService purchaseOrderService;

    @MockBean
    private ApplicationTelemetry telemetry;

    @Test
    void returnsPurchaseOrders() throws Exception {
        AdminPurchaseOrderSummary order = new AdminPurchaseOrderSummary(
                1L, 3L, "Aotearoa Cellars", 12,
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 21));
        when(purchaseOrderService.listPurchaseOrders()).thenReturn(Collections.singletonList(order));

        mockMvc.perform(get("/api/admin/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wineName").value("Aotearoa Cellars"))
                .andExpect(jsonPath("$[0].quantity").value(12))
                .andExpect(jsonPath("$[0].deliveryDate").value("2026-08-21"));
    }

    @Test
    void createsPurchaseOrder() throws Exception {
        AdminPurchaseOrderSummary order = new AdminPurchaseOrderSummary(
                1L, 3L, "Aotearoa Cellars", 12,
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 21));
        when(purchaseOrderService.createPurchaseOrder(3L, 12)).thenReturn(order);

        mockMvc.perform(post("/api/admin/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wineId\":3,\"quantity\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deliveryDate").value("2026-08-21"));
    }

    @Test
    void receivesPurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/admin/purchase-orders/1/receive"))
                .andExpect(status().isNoContent());

        verify(purchaseOrderService).receivePurchaseOrder(1L);
    }

    @Test
    void rejectsNonPositiveQuantity() throws Exception {
        mockMvc.perform(post("/api/admin/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wineId\":3,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }
}