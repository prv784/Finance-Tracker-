package com.financetracker.service.impl;

import com.financetracker.dto.request.ExpenseRequest;
import com.financetracker.dto.response.CategoryResponse;
import com.financetracker.dto.response.ExpenseResponse;
import com.financetracker.entity.Category;
import com.financetracker.entity.Expense;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.ExpenseRepository;
import com.financetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseServiceImpl {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = getCurrentUser();
        Expense expense = buildExpense(request, user);
        return mapToResponse(expenseRepository.save(expense));
    }

    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {

        User user = getCurrentUser();

        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        expense.setTitle(request.getTitle());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setDate(request.getDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNotes(request.getNotes());
        expense.setRecurring(request.isRecurring());

        // Category
        if (request.getCategoryId() != null) {

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Category", request.getCategoryId()));

            expense.setCategory(category);
        }

        // Safe enum handling
        if (request.getRecurrenceType() != null &&
                !request.getRecurrenceType().trim().isEmpty()) {

            expense.setRecurrenceType(
                    Expense.RecurrenceType.valueOf(
                            request.getRecurrenceType()
                                    .trim()
                                    .toUpperCase()
                    )
            );
        }

        return mapToResponse(expenseRepository.save(expense));
    }

    public void deleteExpense(Long id) {

        User user = getCurrentUser();

        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId
    ) {

        User user = getCurrentUser();

        List<Expense> expenses;

        if (startDate != null && endDate != null && categoryId != null) {

            expenses =
                    expenseRepository.findByUserIdAndDateBetweenAndCategoryIdOrderByDateDesc(
                            user.getId(),
                            startDate,
                            endDate,
                            categoryId
                    );

        } else if (startDate != null && endDate != null) {

            expenses =
                    expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                            user.getId(),
                            startDate,
                            endDate
                    );

        } else if (categoryId != null) {

            expenses =
                    expenseRepository.findByUserIdAndCategoryIdOrderByDateDesc(
                            user.getId(),
                            categoryId
                    );

        } else {

            expenses =
                    expenseRepository.findByUserIdOrderByDateDesc(user.getId());
        }

        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {

        User user = getCurrentUser();

        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        return mapToResponse(expense);
    }

    private Expense buildExpense(ExpenseRequest request, User user) {

        Expense.ExpenseBuilder builder = Expense.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .date(request.getDate())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .isRecurring(request.isRecurring())
                .user(user);

        // Category
        if (request.getCategoryId() != null) {

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Category",
                                    request.getCategoryId()
                            ));

            builder.category(category);
        }

        // Safe enum handling
        if (request.getRecurrenceType() != null &&
                !request.getRecurrenceType().trim().isEmpty()) {

            builder.recurrenceType(
                    Expense.RecurrenceType.valueOf(
                            request.getRecurrenceType()
                                    .trim()
                                    .toUpperCase()
                    )
            );
        }

        return builder.build();
    }

    public ExpenseResponse mapToResponse(Expense expense) {

        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .category(
                        expense.getCategory() != null
                                ? mapCategoryToResponse(expense.getCategory())
                                : null
                )
                .paymentMethod(expense.getPaymentMethod())
                .notes(expense.getNotes())
                .isRecurring(expense.isRecurring())
                .recurrenceType(
                        expense.getRecurrenceType() != null
                                ? expense.getRecurrenceType().name()
                                : null
                )
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private CategoryResponse mapCategoryToResponse(Category category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .type(category.getType().name())
                .isDefault(category.isDefault())
                .build();
    }

    User getCurrentUser() {

        String email =
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }
}