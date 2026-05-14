package com.financetracker.service.impl;

import com.financetracker.dto.request.IncomeRequest;
import com.financetracker.dto.response.IncomeResponse;
import com.financetracker.entity.Income;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.IncomeRepository;
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
public class IncomeServiceImpl {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    public IncomeResponse createIncome(IncomeRequest request) {
        User user = getCurrentUser();
        Income income = buildIncome(request, user);
        return mapToResponse(incomeRepository.save(income));
    }

    public IncomeResponse updateIncome(Long id, IncomeRequest request) {
        User user = getCurrentUser();

        Income income = incomeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));

        income.setTitle(request.getTitle());
        income.setDescription(request.getDescription());
        income.setAmount(request.getAmount());
        income.setDate(request.getDate());
        income.setNotes(request.getNotes());
        income.setRecurring(request.isRecurring());

        // Handle source safely
        if (request.getSource() != null &&
                !request.getSource().trim().isEmpty()) {

            try {
                income.setSource(
                        Income.IncomeSource.valueOf(
                                request.getSource().trim().toUpperCase()
                        )
                );
            } catch (IllegalArgumentException e) {
                income.setSource(Income.IncomeSource.OTHER);
            }
        } else {
            income.setSource(Income.IncomeSource.OTHER);
        }

        // Handle recurrence type safely
        if (request.getRecurrenceType() != null &&
                !request.getRecurrenceType().trim().isEmpty()) {

            try {
                income.setRecurrenceType(
                        Income.RecurrenceType.valueOf(
                                request.getRecurrenceType().trim().toUpperCase()
                        )
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid recurrence type. Allowed values: DAILY, WEEKLY, MONTHLY, YEARLY"
                );
            }

        } else {
            income.setRecurrenceType(null);
        }

        return mapToResponse(incomeRepository.save(income));
    }

    public void deleteIncome(Long id) {
        User user = getCurrentUser();

        Income income = incomeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));

        incomeRepository.delete(income);
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> getIncomes(LocalDate startDate, LocalDate endDate) {

        User user = getCurrentUser();
        List<Income> incomes;

        if (startDate != null && endDate != null) {

            incomes = incomeRepository
                    .findByUserIdAndDateBetweenOrderByDateDesc(
                            user.getId(),
                            startDate,
                            endDate
                    );

        } else {

            incomes = incomeRepository
                    .findByUserIdOrderByDateDesc(user.getId());
        }

        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(Long id) {

        User user = getCurrentUser();

        Income income = incomeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));

        return mapToResponse(income);
    }

    private Income buildIncome(IncomeRequest request, User user) {

        // Default source
        Income.IncomeSource source = Income.IncomeSource.OTHER;

        if (request.getSource() != null &&
                !request.getSource().trim().isEmpty()) {

            try {
                source = Income.IncomeSource.valueOf(
                        request.getSource().trim().toUpperCase()
                );
            } catch (IllegalArgumentException ignored) {
                source = Income.IncomeSource.OTHER;
            }
        }

        Income.IncomeBuilder builder = Income.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .date(request.getDate())
                .source(source)
                .notes(request.getNotes())
                .isRecurring(request.isRecurring())
                .user(user);

        // Handle recurrence type safely
        if (request.getRecurrenceType() != null &&
                !request.getRecurrenceType().trim().isEmpty()) {

            try {

                builder.recurrenceType(
                        Income.RecurrenceType.valueOf(
                                request.getRecurrenceType().trim().toUpperCase()
                        )
                );

            } catch (IllegalArgumentException e) {

                throw new IllegalArgumentException(
                        "Invalid recurrence type. Allowed values: DAILY, WEEKLY, MONTHLY, YEARLY"
                );
            }
        }

        return builder.build();
    }

    public IncomeResponse mapToResponse(Income income) {

        return IncomeResponse.builder()
                .id(income.getId())
                .title(income.getTitle())
                .description(income.getDescription())
                .amount(income.getAmount())
                .date(income.getDate())
                .source(income.getSource() != null
                        ? income.getSource().name()
                        : null)
                .notes(income.getNotes())
                .isRecurring(income.isRecurring())
                .recurrenceType(
                        income.getRecurrenceType() != null
                                ? income.getRecurrenceType().name()
                                : null
                )
                .createdAt(income.getCreatedAt())
                .updatedAt(income.getUpdatedAt())
                .build();
    }

    User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }
}