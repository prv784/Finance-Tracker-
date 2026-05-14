package com.financetracker.controller;

import com.financetracker.dto.request.BudgetRequest;
import com.financetracker.dto.response.ApiResponse;
import com.financetracker.dto.response.BudgetResponse;
import com.financetracker.service.impl.BudgetServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {
    private final BudgetServiceImpl budgetService;

    @PostMapping
    @Operation(summary = "Create budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.createBudget(request), "Budget created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.updateBudget(id, request), "Budget updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted"));
    }

    @GetMapping
    @Operation(summary = "Get budgets, optionally filtered by month/year")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgets(month, year), "Budgets fetched"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgetById(id), "Budget fetched"));
    }
}
