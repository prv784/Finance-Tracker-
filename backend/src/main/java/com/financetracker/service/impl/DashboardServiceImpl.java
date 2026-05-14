package com.financetracker.service.impl;

import com.financetracker.dto.response.BudgetResponse;
import com.financetracker.dto.response.DashboardResponse;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final ExpenseServiceImpl expenseService;
    private final IncomeServiceImpl incomeService;
    private final BudgetServiceImpl budgetService;

    public DashboardResponse getDashboard(int year, int month) {
        User user = getCurrentUser();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        // === Algorithm: Compute totals ===
        BigDecimal totalIncome = Optional.ofNullable(
                incomeRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate))
                .orElse(BigDecimal.ZERO);

        BigDecimal totalExpenses = Optional.ofNullable(
                expenseRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate))
                .orElse(BigDecimal.ZERO);

        BigDecimal totalSavings = totalIncome.subtract(totalExpenses);

        // === Algorithm: Savings Rate = (Savings / Income) * 100 ===
        double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? totalSavings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // === Algorithm: Category-wise expense breakdown with percentage ===
        List<Object[]> categoryData = expenseRepository.findCategoryWiseSummaryNative(
                user.getId(), startDate, endDate);
        List<DashboardResponse.CategorySummary> expenseByCategory = buildCategorySummary(
                categoryData, totalExpenses);

        // === Algorithm: 12-month trend analysis ===
        List<DashboardResponse.MonthlyData> monthlyData = buildMonthlyData(user.getId(), year);

        // Recent transactions (last 10)
        var recentExpenses = expenseRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream().limit(10).map(expenseService::mapToResponse).collect(Collectors.toList());

        var recentIncomes = incomeRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream().limit(5).map(incomeService::mapToResponse).collect(Collectors.toList());

        // Active budgets for current month
        List<BudgetResponse> activeBudgets = budgetRepository
                .findByUserIdAndMonthAndYear(user.getId(), month, year)
                .stream().map(budgetService::mapToResponse).collect(Collectors.toList());

        long totalTransactions = expenseRepository.countByUserIdAndDateBetween(
                user.getId(), startDate, endDate);

        return DashboardResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .totalSavings(totalSavings)
                .savingsRate(Math.max(savingsRate, 0.0))
                .expenseByCategory(expenseByCategory)
                .monthlyData(monthlyData)
                .recentExpenses(recentExpenses)
                .recentIncomes(recentIncomes)
                .activeBudgets(activeBudgets)
                .totalTransactions((int) totalTransactions)
                .build();
    }

    /**
     * Algorithm: Category Summary Computation
     * 1. For each category row from DB, extract name and total amount
     * 2. Compute percentage = (categoryAmount / totalExpenses) * 100
     * 3. Assign predefined colors for pie chart rendering
     * 4. Sort by amount descending
     */
    private List<DashboardResponse.CategorySummary> buildCategorySummary(
            List<Object[]> categoryData, BigDecimal totalExpenses) {

        String[] colors = {"#667eea", "#764ba2", "#f093fb", "#f5576c", "#4facfe",
                "#00f2fe", "#43e97b", "#38f9d7", "#fa709a", "#fee140"};

        return categoryData.stream()
                .map(row -> {
                    String catName = (String) row[0];
                    BigDecimal amount = new BigDecimal(row[1].toString());
                    double percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    int idx = Math.abs(catName.hashCode()) % colors.length;
                    return DashboardResponse.CategorySummary.builder()
                            .category(catName)
                            .amount(amount)
                            .percentage(Math.round(percentage * 10.0) / 10.0)
                            .color(colors[idx])
                            .build();
                })
                .sorted(Comparator.comparing(DashboardResponse.CategorySummary::getAmount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Algorithm: Monthly Trend Computation
     * 1. Query monthly income and expense totals for the given year
     * 2. Build a map of month -> {income, expense}
     * 3. For each month 1..12, fill in values (0 if no data)
     * 4. Compute savings = income - expenses per month
     * Result enables bar/line charts showing full-year financial trend
     */
    private List<DashboardResponse.MonthlyData> buildMonthlyData(Long userId, int year) {
        List<Object[]> monthlyExpenses = expenseRepository.findMonthlyExpenseSummaryNative(userId, year);
        List<Object[]> monthlyIncomes = incomeRepository.findMonthlyIncomeSummaryNative(userId, year);

        Map<Integer, BigDecimal> expenseMap = new HashMap<>();
        Map<Integer, BigDecimal> incomeMap = new HashMap<>();

        monthlyExpenses.forEach(row -> expenseMap.put(
                ((Number) row[0]).intValue(),
                new BigDecimal(row[1].toString())));

        monthlyIncomes.forEach(row -> incomeMap.put(
                ((Number) row[0]).intValue(),
                new BigDecimal(row[1].toString())));

        List<DashboardResponse.MonthlyData> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal inc = incomeMap.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal exp = expenseMap.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal sav = inc.subtract(exp);

            String monthName = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(DashboardResponse.MonthlyData.builder()
                    .month(monthName)
                    .monthNumber(m)
                    .income(inc)
                    .expenses(exp)
                    .savings(sav)
                    .build());
        }
        return result;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
