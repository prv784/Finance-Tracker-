package com.financetracker.controller;

import com.financetracker.dto.response.ApiResponse;
import com.financetracker.dto.response.DashboardResponse;
import com.financetracker.service.impl.DashboardServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard analytics APIs")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard data for given month/year")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        int y = year == 0 ? now.getYear() : year;
        int m = month == 0 ? now.getMonthValue() : month;
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(y, m), "Dashboard loaded"));
    }
}
