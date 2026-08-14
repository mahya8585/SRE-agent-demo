package com.example.wine.admin;

import com.example.wine.admin.dto.AdminDashboardSummary;
import com.example.wine.admin.service.AdminDashboardService;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService dashboardService;

    @MockBean
    private ApplicationTelemetry telemetry;

    @Test
    void returnsDashboardSummary() throws Exception {
        when(dashboardService.getSummary()).thenReturn(new AdminDashboardSummary(12L, 186400.0, 3L, 148L));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(12))
                .andExpect(jsonPath("$.revenue").value(186400.0))
                .andExpect(jsonPath("$.lowStockCount").value(3))
                .andExpect(jsonPath("$.totalStock").value(148));
    }
}