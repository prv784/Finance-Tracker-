package com.financetracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String icon;

    private String color;

    @NotBlank(message = "Category type is required")
    private String type;
}
