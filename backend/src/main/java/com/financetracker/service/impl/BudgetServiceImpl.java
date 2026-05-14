package com.financetracker.service.impl;

import com.financetracker.dto.request.BudgetRequest;
import com.financetracker.dto.response.BudgetResponse;
import com.financetracker.dto.response.CategoryResponse;
import com.financetracker.entity.Budget;
import com.financetracker.entity.Category;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.BudgetRepository;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.ExpenseRepository;
import com.financetracker.repository.UserRepository;
import com.financetracker.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BudgetServiceImpl {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailService emailService;

    public BudgetResponse createBudget(BudgetRequest request) {
        User user = getCurrentUser();
        Budget budget = buildBudget(request, user);
        return mapToResponse(budgetRepository.save(budget));
    }

    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));

        budget.setName(request.getName());
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budget.setAlertThreshold(request.getAlertThreshold());
        budget.setAlertSent(false);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            budget.setCategory(category);
        }

        return mapToResponse(budgetRepository.save(budget));
    }

    public void deleteBudget(Long id) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Integer month, Integer year) {
        User user = getCurrentUser();
        List<Budget> budgets;
        if (month != null && year != null) {
            budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        } else {
            budgets = budgetRepository.findByUserIdOrderByYearDescMonthDesc(user.getId());
        }
        return budgets.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        return mapToResponse(budget);
    }

    // Runs every hour to check budget alerts
    @Scheduled(fixedRate = 3600000)
    public void checkBudgetAlerts() {
        List<Budget> budgetsToCheck = budgetRepository.findByUserIdAndAlertSentFalse(null);
        for (Budget budget : budgetsToCheck) {
            try {
                BigDecimal spent = calculateSpent(budget);
                if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    double percentage = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    if (percentage >= budget.getAlertThreshold().doubleValue()) {
                        emailService.sendBudgetAlertEmail(
                                budget.getUser().getEmail(),
                                budget.getUser().getFirstName(),
                                budget.getName(), spent, budget.getAmount(), percentage);
                        budget.setAlertSent(true);
                        budgetRepository.save(budget);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking budget alert for budget {}: {}", budget.getId(), e.getMessage());
            }
        }
    }

    private BigDecimal calculateSpent(Budget budget) {
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = YearMonth.of(budget.getYear(), budget.getMonth()).atEndOfMonth();

        if (budget.getCategory() != null) {
            BigDecimal result = expenseRepository.sumAmountByUserIdAndCategoryIdAndDateBetween(
                    budget.getUser().getId(), budget.getCategory().getId(), startDate, endDate);
            return result != null ? result : BigDecimal.ZERO;
        } else {
            BigDecimal result = expenseRepository.sumAmountByUserIdAndDateBetween(
                    budget.getUser().getId(), startDate, endDate);
            return result != null ? result : BigDecimal.ZERO;
        }
    }

    private Budget buildBudget(BudgetRequest request, User user) {
        Budget.BudgetBuilder builder = Budget.builder()
                .name(request.getName())
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .alertThreshold(request.getAlertThreshold())
                .user(user);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            builder.category(category);
        }

        return builder.build();
    }

    public BudgetResponse mapToResponse(Budget budget) {
        BigDecimal spent = calculateSpent(budget);
        BigDecimal remaining = budget.getAmount().subtract(spent);
        double percentageUsed = budget.getAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BudgetResponse.builder()
                .id(budget.getId())
                .name(budget.getName())
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .category(budget.getCategory() != null ? CategoryResponse.builder()
                        .id(budget.getCategory().getId())
                        .name(budget.getCategory().getName())
                        .icon(budget.getCategory().getIcon())
                        .color(budget.getCategory().getColor())
                        .type(budget.getCategory().getType().name())
                        .build() : null)
                .alertThreshold(budget.getAlertThreshold())
                .alertSent(budget.isAlertSent())
                .spent(spent)
                .remaining(remaining)
                .percentageUsed(Math.min(percentageUsed, 100.0))
                .createdAt(budget.getCreatedAt())
                .build();
    }

    User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
