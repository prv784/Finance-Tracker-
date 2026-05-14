package com.financetracker.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private String type;
    private boolean isDefault;
}
