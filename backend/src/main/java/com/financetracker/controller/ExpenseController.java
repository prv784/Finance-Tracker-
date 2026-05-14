package com.financetracker.controller;

import com.financetracker.dto.request.ExpenseRequest;
import com.financetracker.dto.response.ApiResponse;
import com.financetracker.dto.response.ExpenseResponse;
import com.financetracker.service.impl.ExpenseServiceImpl;
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
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseServiceImpl expenseService;

    @PostMapping
    @Operation(summary = "Create new expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.createExpense(request), "Expense created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.updateExpense(id, request), "Expense updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted"));
    }

    @GetMapping
    @Operation(summary = "Get all expenses with optional filters")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getExpenses(startDate, endDate, categoryId), "Expenses fetched"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpenseById(id), "Expense fetched"));
    }
}
