package com.example.wine.admin.service;

import com.example.wine.admin.dto.AdminDashboardSummary;
import com.example.wine.repository.CustomerOrderRepository;
import com.example.wine.repository.WineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private WineRepository wineRepository;

    @InjectMocks
    private AdminDashboardService dashboardService;

    @Test
    void buildsSummaryFromRepositoryAggregates() {
        when(customerOrderRepository.count()).thenReturn(12L);
        when(customerOrderRepository.calculateTotalRevenue()).thenReturn(186400.0);
        when(wineRepository.countLowStockWines()).thenReturn(3L);
        when(wineRepository.calculateTotalStock()).thenReturn(148L);

        AdminDashboardSummary summary = dashboardService.getSummary();

        assertEquals(12L, summary.getOrderCount());
        assertEquals(186400.0, summary.getRevenue());
        assertEquals(3L, summary.getLowStockCount());
        assertEquals(148L, summary.getTotalStock());
    }
}