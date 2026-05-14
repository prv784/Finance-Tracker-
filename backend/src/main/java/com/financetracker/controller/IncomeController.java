package com.financetracker.controller;

import com.financetracker.dto.request.IncomeRequest;
import com.financetracker.dto.response.ApiResponse;
import com.financetracker.dto.response.IncomeResponse;
import com.financetracker.service.impl.IncomeServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/income")
@RequiredArgsConstructor
@Tag(name = "Income", description = "Income management APIs")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    private final IncomeServiceImpl incomeService;

    @PostMapping
    @Operation(summary = "Create new income entry")
    public ResponseEntity<ApiResponse<IncomeResponse>> createIncome(@Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(incomeService.createIncome(request), "Income added"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update income entry")
    public ResponseEntity<ApiResponse<IncomeResponse>> updateIncome(
            @PathVariable Long id, @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(incomeService.updateIncome(id, request), "Income updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete income entry")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(@PathVariable Long id) {
        incomeService.deleteIncome(id);
        return ResponseEntity.ok(ApiResponse.success("Income deleted"));
    }

    @GetMapping
    @Operation(summary = "Get all income with optional date filter")
    public ResponseEntity<ApiResponse<List<IncomeResponse>>> getIncomes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(incomeService.getIncomes(startDate, endDate), "Income fetched"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get income by ID")
    public ResponseEntity<ApiResponse<IncomeResponse>> getIncomeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(incomeService.getIncomeById(id), "Income fetched"));
    }
}
