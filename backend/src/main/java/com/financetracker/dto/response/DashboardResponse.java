package com.financetracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal totalSavings;
    private double savingsRate;
    private List<CategorySummary> expenseByCategory;
    private List<MonthlyData> monthlyData;
    private List<ExpenseResponse> recentExpenses;
    private List<IncomeResponse> recentIncomes;
    private List<BudgetResponse> activeBudgets;
    private int totalTransactions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategorySummary {
        private String category;
        private BigDecimal amount;
        private double percentage;
        private String color;
        private String icon;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyData {
        private String month;
        private int monthNumber;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal savings;
    }
}
