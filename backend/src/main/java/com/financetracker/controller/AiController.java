package com.financetracker.controller;

import com.financetracker.dto.request.AiChatRequest;
import com.financetracker.dto.response.AiAnalysisResponse;
import com.financetracker.dto.response.ApiResponse;
import com.financetracker.service.impl.AiServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features", description = "AI-powered finance analysis APIs")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiServiceImpl aiService;

    @GetMapping("/analyze")
    @Operation(summary = "AI spending analysis for a given month/year")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> analyzeSpending(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        LocalDate now = LocalDate.now();
        int m = month == 0 ? now.getMonthValue() : month;
        int y = year == 0 ? now.getYear() : year;
        return ResponseEntity.ok(ApiResponse.success(aiService.analyzeSpending(m, y), "Analysis complete"));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI finance assistant")
    public ResponseEntity<ApiResponse<String>> chat(@Valid @RequestBody AiChatRequest request) {
        String reply = aiService.chat(request.getMessage(), request.getConversationId());
        return ResponseEntity.ok(ApiResponse.success(reply, "Response generated"));
    }

    @PostMapping("/categorize")
    @Operation(summary = "AI-powered expense categorization")
    public ResponseEntity<ApiResponse<String>> categorize(@RequestBody Map<String, String> body) {
        String category = aiService.categorizeExpense(body.get("title"), body.get("description"));
        return ResponseEntity.ok(ApiResponse.success(category, "Category suggested"));
    }
}
