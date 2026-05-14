package com.financetracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank(message = "Message is required")
    private String message;

    private String conversationId;
}
