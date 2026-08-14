package com.example.wine.admin;

import com.example.wine.admin.dto.AdminDashboardSummary;
import com.example.wine.admin.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public AdminDashboardSummary getSummary() {
        return dashboardService.getSummary();
    }
}