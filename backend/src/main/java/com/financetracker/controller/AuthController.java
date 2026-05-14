package com.financetracker.controller;

import com.financetracker.dto.request.*;
import com.financetracker.dto.response.ApiResponse;
import com.financetracker.dto.response.AuthResponse;
import com.financetracker.service.impl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth management APIs")
public class AuthController {

    private final AuthServiceImpl authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration successful. Please verify your email."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP sent to email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@RequestBody Map<String, String> body) {
        AuthResponse response = authService.verifyOtp(body.get("email"), body.get("otp"));
        return ResponseEntity.ok(ApiResponse.success(response, "Account verified successfully!"));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP to email")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestBody Map<String, String> body) {
        authService.resendOtp(body.get("email"));
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
    }
}
