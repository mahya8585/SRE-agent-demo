package com.example.wine.admin.order;

import com.example.wine.telemetry.ApplicationTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrderService orderService;

    @MockBean
    private ApplicationTelemetry telemetry;

    @Test
    void returnsOrdersNewestFirst() throws Exception {
        AdminOrderSummary order = new AdminOrderSummary(
                1L, "MV-12345678", "CONFIRMED", "山田 太郎", 2, 15600.0,
                OffsetDateTime.parse("2026-08-14T10:15:30+09:00"));
        when(orderService.listOrders()).thenReturn(Collections.singletonList(order));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("MV-12345678"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].customerName").value("山田 太郎"))
                .andExpect(jsonPath("$[0].total").value(15600.0));
    }

    @Test
    void updatesOrderStatus() throws Exception {
        AdminOrderSummary order = new AdminOrderSummary(
                1L, "MV-12345678", "SHIPPED", "山田 太郎", 2, 15600.0,
                OffsetDateTime.parse("2026-08-14T10:15:30+09:00"));
        when(orderService.updateStatus(1L, "SHIPPED")).thenReturn(order);

        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}