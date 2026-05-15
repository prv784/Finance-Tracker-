package com.financetracker.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.dto.response.AiAnalysisResponse;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.ExpenseRepository;
import com.financetracker.repository.IncomeRepository;
import com.financetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiServiceImpl {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final RestTemplate restTemplate;

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.gemini.model}")
    private String geminiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisResponse analyzeSpending(int month, int year) {

        User user = getCurrentUser();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        BigDecimal totalIncome = Optional.ofNullable(
                incomeRepository.sumAmountByUserIdAndDateBetween(
                        user.getId(),
                        startDate,
                        endDate
                )
        ).orElse(BigDecimal.ZERO);

        BigDecimal totalExpenses = Optional.ofNullable(
                expenseRepository.sumAmountByUserIdAndDateBetween(
                        user.getId(),
                        startDate,
                        endDate
                )
        ).orElse(BigDecimal.ZERO);

        List<Object[]> categoryData =
                expenseRepository.findCategoryWiseSummaryNative(
                        user.getId(),
                        startDate,
                        endDate
                );

        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("Monthly Financial Data:\n");
        contextBuilder.append("Total Income: $")
                .append(totalIncome)
                .append("\n");

        contextBuilder.append("Total Expenses: $")
                .append(totalExpenses)
                .append("\n");

        contextBuilder.append("Savings: $")
                .append(totalIncome.subtract(totalExpenses))
                .append("\n");

        contextBuilder.append("Category Breakdown:\n");

        for (Object[] row : categoryData) {

            contextBuilder.append("- ")
                    .append(row[0])
                    .append(": $")
                    .append(row[1])
                    .append("\n");
        }

        String prompt = """
                You are an AI financial advisor.

                Analyze the following financial data and provide:

                1. Summary
                2. Spending insights
                3. Savings suggestions
                4. Warnings
                5. Spending pattern

                Respond ONLY in valid JSON format:

                {
                  "summary": "",
                  "insights": [],
                  "suggestions": [],
                  "warnings": [],
                  "spendingPattern": ""
                }

                Financial Data:
                %s
                """.formatted(contextBuilder.toString());

        double healthScore =
                calculateHealthScore(
                        totalIncome,
                        totalExpenses,
                        categoryData
                );

        String healthGrade;

        if (healthScore >= 80) {
            healthGrade = "A";
        } else if (healthScore >= 60) {
            healthGrade = "B";
        } else if (healthScore >= 40) {
            healthGrade = "C";
        } else {
            healthGrade = "D";
        }

        try {

            String aiResponse = callGemini(prompt);

            Map<String, Object> parsed =
                    parseJsonResponse(aiResponse);

            return AiAnalysisResponse.builder()
                    .summary(
                            (String) parsed.getOrDefault(
                                    "summary",
                                    "Financial analysis completed."
                            )
                    )
                    .insights(castList(parsed.get("insights")))
                    .suggestions(castList(parsed.get("suggestions")))
                    .warnings(castList(parsed.get("warnings")))
                    .spendingPattern(
                            (String) parsed.getOrDefault(
                                    "spendingPattern",
                                    "Balanced Spender"
                            )
                    )
                    .healthScore(healthScore)
                    .healthGrade(healthGrade)
                    .build();

        } catch (Exception e) {

            log.error(
                    "AI analysis error: {}",
                    e.getMessage(),
                    e
            );

            return buildFallbackAnalysis(
                    totalIncome,
                    totalExpenses,
                    categoryData,
                    healthScore,
                    healthGrade
            );
        }
    }

    public String chat(String message, String userId) {

        User user = getCurrentUser();

        LocalDate startDate =
                LocalDate.now().withDayOfMonth(1);

        LocalDate endDate =
                YearMonth.now().atEndOfMonth();

        BigDecimal totalIncome = Optional.ofNullable(
                incomeRepository.sumAmountByUserIdAndDateBetween(
                        user.getId(),
                        startDate,
                        endDate
                )
        ).orElse(BigDecimal.ZERO);

        BigDecimal totalExpenses = Optional.ofNullable(
                expenseRepository.sumAmountByUserIdAndDateBetween(
                        user.getId(),
                        startDate,
                        endDate
                )
        ).orElse(BigDecimal.ZERO);

        String systemPrompt = """
                You are a smart AI finance assistant.

                User Financial Data:
                Income: $%s
                Expenses: $%s
                Savings: $%s

                Give short, practical and helpful finance advice.
                """.formatted(
                totalIncome,
                totalExpenses,
                totalIncome.subtract(totalExpenses)
        );

        try {

            return callGemini(
                    systemPrompt
                            + "\n\nUser Question:\n"
                            + message
            );

        } catch (Exception e) {

            log.error(
                    "AI chat error: {}",
                    e.getMessage(),
                    e
            );

            return "AI service is temporarily unavailable. "
                    + "Current savings this month: $"
                    + totalIncome.subtract(totalExpenses);
        }
    }

    public String categorizeExpense(
            String title,
            String description
    ) {

        String prompt = """
                Categorize this expense.

                Title: %s
                Description: %s

                Choose ONLY one category:

                Food & Dining
                Transportation
                Shopping
                Entertainment
                Healthcare
                Utilities
                Housing
                Education
                Other
                """.formatted(
                title,
                description != null ? description : ""
        );

        try {

            return callGemini(prompt).trim();

        } catch (Exception e) {

            log.error(
                    "Categorization error: {}",
                    e.getMessage()
            );

            return "Other";
        }
    }

    private String callGemini(String prompt) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart =
                new HashMap<>();

        textPart.put("text", prompt);

        Map<String, Object> parts =
                new HashMap<>();

        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody =
                new HashMap<>();

        requestBody.put(
                "contents",
                List.of(parts)
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(requestBody, headers);

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                        + geminiModel
                        + ":generateContent?key="
                        + geminiApiKey;

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

        String responseBody = response.getBody();

        log.info(
                "Gemini API Response: {}",
                responseBody
        );

        JsonNode root =
                objectMapper.readTree(responseBody);

        if (root.has("error")) {

            String errorMessage = root
                    .path("error")
                    .path("message")
                    .asText();

            throw new RuntimeException(
                    "Gemini API Error: "
                            + errorMessage
            );
        }

        JsonNode candidates =
                root.path("candidates");

        if (!candidates.isArray()
                || candidates.size() == 0) {

            throw new RuntimeException(
                    "No candidates returned from Gemini API"
            );
        }

        JsonNode textNode = candidates
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text");

        if (textNode.isMissingNode()) {

            throw new RuntimeException(
                    "Invalid Gemini response format"
            );
        }

        return textNode.asText();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(
            String json
    ) {

        try {

            String cleaned = json
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(
                    cleaned,
                    Map.class
            );

        } catch (Exception e) {

            log.error(
                    "JSON parse error: {}",
                    e.getMessage()
            );

            return new HashMap<>();
        }
    }

    private List<String> castList(Object obj) {

        if (obj instanceof List<?>) {

            return ((List<?>) obj)
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private double calculateHealthScore(
            BigDecimal income,
            BigDecimal expenses,
            List<Object[]> categories
    ) {

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return 50.0;
        }

        double score = 0;

        double savingsRate =
                1.0 - expenses.divide(
                        income,
                        4,
                        RoundingMode.HALF_UP
                ).doubleValue();

        if (savingsRate >= 0.30) {
            score += 40;
        } else if (savingsRate >= 0.20) {
            score += 30;
        } else if (savingsRate >= 0.10) {
            score += 20;
        } else {
            score += 10;
        }

        if (!categories.isEmpty()
                && expenses.compareTo(BigDecimal.ZERO) > 0) {

            double entropy = 0;

            for (Object[] row : categories) {

                double p =
                        new BigDecimal(
                                row[1].toString()
                        )
                                .divide(
                                        expenses,
                                        4,
                                        RoundingMode.HALF_UP
                                )
                                .doubleValue();

                if (p > 0) {
                    entropy -=
                            p * Math.log(p) / Math.log(2);
                }
            }

            double maxEntropy =
                    Math.log(categories.size())
                            / Math.log(2);

            double normalizedEntropy =
                    maxEntropy > 0
                            ? entropy / maxEntropy
                            : 0;

            score += normalizedEntropy * 30;
        }

        score += 20;

        double expenseRatio =
                expenses.divide(
                        income,
                        4,
                        RoundingMode.HALF_UP
                ).doubleValue();

        if (expenseRatio <= 0.70) {
            score += 10;
        }

        return Math.min(
                100,
                Math.max(0, score)
        );
    }

    private AiAnalysisResponse buildFallbackAnalysis(
            BigDecimal income,
            BigDecimal expenses,
            List<Object[]> categories,
            double healthScore,
            String grade
    ) {

        List<String> insights =
                new ArrayList<>();

        List<String> suggestions =
                new ArrayList<>();

        List<String> warnings =
                new ArrayList<>();

        double savingsRate =
                income.compareTo(BigDecimal.ZERO) > 0
                        ? (
                        1.0 - expenses.divide(
                                income,
                                4,
                                RoundingMode.HALF_UP
                        ).doubleValue()
                ) * 100
                        : 0;

        insights.add(
                "You saved "
                        + String.format(
                        "%.1f",
                        savingsRate
                )
                        + "% this month."
        );

        insights.add(
                "Total expenses: $" + expenses
        );

        if (!categories.isEmpty()) {

            insights.add(
                    "Top spending category: "
                            + categories.get(0)[0]
            );
        }

        if (savingsRate < 10) {

            warnings.add(
                    "Low savings rate detected."
            );
        }

        suggestions.add(
                "Track daily expenses regularly."
        );

        suggestions.add(
                "Try saving at least 20% monthly."
        );

        suggestions.add(
                "Reduce unnecessary spending."
        );

        return AiAnalysisResponse.builder()
                .summary(
                        "Monthly financial analysis completed."
                )
                .insights(insights)
                .suggestions(suggestions)
                .warnings(warnings)
                .spendingPattern(
                        savingsRate >= 20
                                ? "Conscious Saver"
                                : "Active Spender"
                )
                .healthScore(healthScore)
                .healthGrade(grade)
                .build();
    }

    private User getCurrentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }
}
