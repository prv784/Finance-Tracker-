package com.financetracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetResponse {
    private Long id;
    private String name;
    private BigDecimal amount;
    private Integer month;
    private Integer year;
    private CategoryResponse category;
    private BigDecimal alertThreshold;
    private boolean alertSent;
    private BigDecimal spent;
    private BigDecimal remaining;
    private double percentageUsed;
    private LocalDateTime createdAt;
}
