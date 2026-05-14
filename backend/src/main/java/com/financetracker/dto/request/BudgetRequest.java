package com.financetracker.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetRequest {
    @NotBlank(message = "Budget name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Month is required")
    @Min(1) @Max(12)
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(2000)
    private Integer year;

    private Long categoryId;

    @DecimalMin(value = "1") @DecimalMax(value = "100")
    private BigDecimal alertThreshold = new BigDecimal("80");
}
