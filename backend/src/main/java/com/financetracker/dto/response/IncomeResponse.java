package com.financetracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IncomeResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private String source;
    private String notes;
    private boolean isRecurring;
    private String recurrenceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
