package com.financetracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private CategoryResponse category;
    private String paymentMethod;
    private String notes;
    private boolean isRecurring;
    private String recurrenceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
