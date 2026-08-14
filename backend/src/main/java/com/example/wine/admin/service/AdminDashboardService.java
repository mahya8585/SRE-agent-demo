package com.example.wine.admin.service;

import com.example.wine.admin.dto.AdminDashboardSummary;
import com.example.wine.repository.CustomerOrderRepository;
import com.example.wine.repository.WineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final CustomerOrderRepository customerOrderRepository;
    private final WineRepository wineRepository;

    public AdminDashboardService(CustomerOrderRepository customerOrderRepository, WineRepository wineRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.wineRepository = wineRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummary getSummary() {
        return new AdminDashboardSummary(
                customerOrderRepository.count(),
                customerOrderRepository.calculateTotalRevenue(),
                wineRepository.countLowStockWines(),
                wineRepository.calculateTotalStock()
        );
    }
}