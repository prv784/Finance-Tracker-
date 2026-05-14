package com.financetracker.service.impl;

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

    @Value("${app.openai.api-key}")
    private String openAiApiKey;

    @Value("${app.openai.model}")
    private String openAiModel;

    @Value("${app.openai.max-tokens}")
    private int maxTokens;

    /**
     * Algorithm: Financial Health Score
     * Score = weighted sum of:
     * - Savings Rate (40%): ideal >= 20%, penalized below 10%
     * - Budget Adherence (30%): % of budgets not exceeded
     * - Expense Diversity (20%): entropy of expense categories
     * - Income Stability (10%): variance of monthly income
     * Grade: A (80-100), B (60-79), C (40-59), D (< 40)
     */
    public AiAnalysisResponse analyzeSpending(int month, int year) {
        User user = getCurrentUser();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        BigDecimal totalIncome = Optional.ofNullable(
                incomeRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalExpenses = Optional.ofNullable(
                expenseRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate))
                .orElse(BigDecimal.ZERO);

        List<Object[]> categoryData = expenseRepository.findCategoryWiseSummaryNative(
                user.getId(), startDate, endDate);

        // Build context for AI
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Monthly Financial Data:\n");
        contextBuilder.append("Total Income: $").append(totalIncome).append("\n");
        contextBuilder.append("Total Expenses: $").append(totalExpenses).append("\n");
        contextBuilder.append("Net Savings: $").append(totalIncome.subtract(totalExpenses)).append("\n");

        double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? totalExpenses.divide(totalIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        contextBuilder.append("Savings Rate: ").append(String.format("%.1f", 100 - savingsRate)).append("%\n");
        contextBuilder.append("Category Breakdown:\n");
        categoryData.forEach(row -> contextBuilder
                .append("- ").append(row[0]).append(": $").append(row[1]).append("\n"));

        String prompt = """
            You are a personal finance advisor AI. Analyze this financial data and provide:
            1. A brief summary (2-3 sentences)
            2. 3-5 specific insights about spending patterns
            3. 3-5 actionable saving suggestions
            4. Any warnings about overspending or risky patterns
            5. The overall spending pattern category (e.g., "Conservative Spender", "Lifestyle Inflated", etc.)
            
            %s
            
            Respond in this exact JSON format:
            {
              "summary": "...",
              "insights": ["...", "..."],
              "suggestions": ["...", "..."],
              "warnings": ["...", "..."],
              "spendingPattern": "..."
            }
            """.formatted(contextBuilder.toString());

        // Calculate local health score as fallback
        double healthScore = calculateHealthScore(totalIncome, totalExpenses, categoryData);
        String healthGrade = healthScore >= 80 ? "A" : healthScore >= 60 ? "B" : healthScore >= 40 ? "C" : "D";

        try {
            String aiResponse = callOpenAI(prompt);
            Map<String, Object> parsed = parseJsonResponse(aiResponse);

            return AiAnalysisResponse.builder()
                    .summary((String) parsed.getOrDefault("summary", "Analysis complete."))
                    .insights(castList(parsed.get("insights")))
                    .suggestions(castList(parsed.get("suggestions")))
                    .warnings(castList(parsed.get("warnings")))
                    .spendingPattern((String) parsed.getOrDefault("spendingPattern", "Moderate Spender"))
                    .healthScore(healthScore)
                    .healthGrade(healthGrade)
                    .build();
        } catch (Exception e) {
            log.error("AI analysis error: {}", e.getMessage());
            return buildFallbackAnalysis(totalIncome, totalExpenses, categoryData, healthScore, healthGrade);
        }
    }

    public String chat(String message, String userId) {
        User user = getCurrentUser();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = YearMonth.now().atEndOfMonth();

        BigDecimal totalIncome = Optional.ofNullable(
                incomeRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalExpenses = Optional.ofNullable(
                expenseRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate))
                .orElse(BigDecimal.ZERO);

        String systemContext = """
            You are a helpful AI personal finance assistant. The user's current month data:
            - Income: $%s, Expenses: $%s, Savings: $%s
            Be concise, friendly, and actionable. Provide specific financial advice.
            """.formatted(totalIncome, totalExpenses, totalIncome.subtract(totalExpenses));

        try {
            return callOpenAIChat(systemContext, message);
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage());
            return "I'm having trouble connecting right now. Your current savings this month are $"
                    + totalIncome.subtract(totalExpenses) + ". Please try again later.";
        }
    }

    public String categorizeExpense(String title, String description) {
        String prompt = """
            Given this expense title: "%s" and description: "%s",
            suggest the best category from: Food & Dining, Transportation, Shopping, 
            Entertainment, Healthcare, Utilities, Housing, Education, Other.
            Respond with ONLY the category name, nothing else.
            """.formatted(title, description != null ? description : "");

        try {
            return callOpenAI(prompt).trim();
        } catch (Exception e) {
            return "Other";
        }
    }

    /**
     * Algorithm: Financial Health Score Calculation
     * Components:
     * 1. Savings component (40 pts max): 
     *    - savingsRate >= 30% -> 40pts
     *    - savingsRate 20-30% -> 30pts
     *    - savingsRate 10-20% -> 20pts
     *    - savingsRate < 10% -> 10pts or 0
     * 2. Expense diversity (30 pts max):
     *    - Shannon entropy of category distribution
     *    - Higher entropy = more balanced spending = higher score
     * 3. Income present (20 pts): has income data
     * 4. Low expense ratio (10 pts): expenses < 70% of income
     */
    private double calculateHealthScore(BigDecimal income, BigDecimal expenses, List<Object[]> categories) {
        if (income.compareTo(BigDecimal.ZERO) == 0) return 50.0;

        double score = 0;

        // Savings component (40 pts)
        double savingsRate = 1.0 - expenses.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        if (savingsRate >= 0.30) score += 40;
        else if (savingsRate >= 0.20) score += 30;
        else if (savingsRate >= 0.10) score += 20;
        else if (savingsRate >= 0) score += 10;

        // Expense diversity using Shannon entropy (30 pts)
        if (!categories.isEmpty() && expenses.compareTo(BigDecimal.ZERO) > 0) {
            double entropy = 0;
            for (Object[] row : categories) {
                double p = new BigDecimal(row[1].toString())
                        .divide(expenses, 4, RoundingMode.HALF_UP).doubleValue();
                if (p > 0) entropy -= p * Math.log(p) / Math.log(2);
            }
            double maxEntropy = Math.log(categories.size()) / Math.log(2);
            double normalizedEntropy = maxEntropy > 0 ? entropy / maxEntropy : 0;
            score += normalizedEntropy * 30;
        }

        // Has income (20 pts)
        score += 20;

        // Expense ratio below 70% (10 pts)
        double expRatio = expenses.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        if (expRatio <= 0.70) score += 10;

        return Math.min(100.0, Math.max(0.0, score));
    }

    private AiAnalysisResponse buildFallbackAnalysis(BigDecimal income, BigDecimal expenses,
            List<Object[]> categories, double healthScore, String grade) {
        List<String> insights = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        double savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? (1.0 - expenses.divide(income, 4, RoundingMode.HALF_UP).doubleValue()) * 100
                : 0;

        insights.add("You saved " + String.format("%.1f", savingsRate) + "% of your income this month.");
        insights.add("Total expenses were $" + expenses + " against income of $" + income + ".");

        if (!categories.isEmpty()) {
            String topCat = (String) categories.get(0)[0];
            insights.add("Your highest spending category is " + topCat + ".");
        }

        if (savingsRate < 10) {
            suggestions.add("Aim to save at least 20% of your income each month.");
            warnings.add("Your savings rate is below the recommended 20%.");
        }
        suggestions.add("Track daily expenses to identify unnecessary spending.");
        suggestions.add("Consider setting up automatic transfers to a savings account.");

        return AiAnalysisResponse.builder()
                .summary("You spent $" + expenses + " this month with $" + income.subtract(expenses) + " saved.")
                .insights(insights)
                .suggestions(suggestions)
                .warnings(warnings)
                .spendingPattern(savingsRate >= 20 ? "Conscious Saver" : "Active Spender")
                .healthScore(healthScore)
                .healthGrade(grade)
                .build();
    }

    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", List.of(message),
                "max_tokens", maxTokens,
                "temperature", 0.7
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, String> messageResp = (Map<String, String>) firstChoice.get("message");
        return messageResp.get("content");
    }

    private String callOpenAIChat(String systemContent, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemContent),
                Map.of("role", "user", "content", userMessage)
        );
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", messages,
                "max_tokens", maxTokens,
                "temperature", 0.8
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, String> messageResp = (Map<String, String>) ((Map<String, Object>) choices.get(0)).get("message");
        return messageResp.get("content");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String jsonStr) {
        // Strip markdown code fences if present
        String clean = jsonStr.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        // Simple JSON parsing using Jackson via ObjectMapper
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, Map.class);
        } catch (Exception e) {
            log.warn("Could not parse AI JSON response: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        if (obj instanceof List) {
            return ((List<?>) obj).stream().map(Object::toString).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
