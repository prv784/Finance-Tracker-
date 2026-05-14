package com.financetracker.service.impl;

import com.financetracker.dto.request.LoginRequest;
import com.financetracker.dto.request.RegisterRequest;
import com.financetracker.dto.request.ResetPasswordRequest;
import com.financetracker.dto.response.AuthResponse;
import com.financetracker.dto.response.UserResponse;
import com.financetracker.entity.Category;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.UserRepository;
import com.financetracker.security.JwtUtils;
import com.financetracker.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    private static final SecureRandom secureRandom = new SecureRandom();

    // Register User
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String otp = generateOtp();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .otpCode(passwordEncoder.encode(otp))
                .otpExpiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        User savedUser = userRepository.save(user);

        createDefaultCategories(savedUser);

        emailService.sendWelcomeEmail(
                savedUser.getEmail(),
                savedUser.getFirstName()
        );

        emailService.sendOtpEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                otp
        );

        log.info("New user registered: {}", savedUser.getEmail());

        return AuthResponse.builder()
                .message("Registration successful. Please verify OTP.")
                .user(mapToUserResponse(savedUser))
                .build();
    }

    // Verify OTP
    public AuthResponse verifyOtp(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getOtpExpiryTime() == null ||
                user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {

            throw new BadRequestException("OTP has expired");
        }

        if (!passwordEncoder.matches(otp, user.getOtpCode())) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setEnabled(true);
        user.setOtpCode(null);
        user.setOtpExpiryTime(null);

        userRepository.save(user);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtUtils.generateToken(userDetails);

        String refreshToken =
                jwtUtils.generateRefreshToken(userDetails);

        log.info("User verified successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("OTP verification successful")
                .user(mapToUserResponse(user))
                .build();
    }

    // Login
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new BadRequestException(
                    "Please verify your account first"
            );
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtUtils.generateToken(userDetails);

        String refreshToken =
                jwtUtils.generateRefreshToken(userDetails);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .user(mapToUserResponse(user))
                .build();
    }

    // Forgot Password
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No account found with email: " + email
                        ));

        String token = UUID.randomUUID().toString();

        user.setResetPasswordToken(token);
        user.setResetTokenExpiry(
                LocalDateTime.now().plusHours(1)
        );

        userRepository.save(user);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getFirstName(),
                token
        );

        log.info("Password reset token generated for: {}", email);
    }

    // Reset Password
    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository
                .findByResetPasswordToken(request.getToken())
                .orElseThrow(() ->
                        new BadRequestException(
                                "Invalid or expired reset token"
                        ));

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry()
                        .isBefore(LocalDateTime.now())) {

            throw new BadRequestException(
                    "Reset token has expired"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);

        log.info("Password reset successful for: {}", user.getEmail());
    }

    // Resend OTP
    public void resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.isEnabled()) {
            throw new BadRequestException(
                    "Account already verified"
            );
        }

        String otp = generateOtp();

        user.setOtpCode(passwordEncoder.encode(otp));
        user.setOtpExpiryTime(
                LocalDateTime.now().plusMinutes(10)
        );

        userRepository.save(user);

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getFirstName(),
                otp
        );

        log.info("OTP resent to: {}", email);
    }

    // Generate Secure OTP
    private String generateOtp() {

        int otp = 100000 + secureRandom.nextInt(900000);

        return String.valueOf(otp);
    }

    // Create Default Categories
    private void createDefaultCategories(User user) {

        List<Category> defaults = List.of(

                Category.builder()
                        .name("Food & Dining")
                        .icon("🍔")
                        .color("#FF6B6B")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Transportation")
                        .icon("🚗")
                        .color("#4ECDC4")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Shopping")
                        .icon("🛍️")
                        .color("#45B7D1")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Entertainment")
                        .icon("🎬")
                        .color("#96CEB4")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Healthcare")
                        .icon("💊")
                        .color("#FFEAA7")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Utilities")
                        .icon("💡")
                        .color("#DDA0DD")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Housing")
                        .icon("🏠")
                        .color("#98D8C8")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Education")
                        .icon("📚")
                        .color("#F7DC6F")
                        .type(Category.CategoryType.EXPENSE)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Salary")
                        .icon("💼")
                        .color("#52BE80")
                        .type(Category.CategoryType.INCOME)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Freelance")
                        .icon("💻")
                        .color("#3498DB")
                        .type(Category.CategoryType.INCOME)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Investment")
                        .icon("📈")
                        .color("#9B59B6")
                        .type(Category.CategoryType.INCOME)
                        .isDefault(true)
                        .user(user)
                        .build(),

                Category.builder()
                        .name("Other")
                        .icon("📌")
                        .color("#95A5A6")
                        .type(Category.CategoryType.BOTH)
                        .isDefault(true)
                        .user(user)
                        .build()
        );

        categoryRepository.saveAll(defaults);
    }

    // Convert User Entity to Response DTO
    private UserResponse mapToUserResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}