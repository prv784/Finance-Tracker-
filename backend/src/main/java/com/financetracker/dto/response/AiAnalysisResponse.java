package com.financetracker.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiAnalysisResponse {
    private String summary;
    private List<String> insights;
    private List<String> suggestions;
    private List<String> warnings;
    private String spendingPattern;
    private double healthScore;
    private String healthGrade;
}
