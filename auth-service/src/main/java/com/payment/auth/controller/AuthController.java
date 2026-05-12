package com.payment.auth.controller;

import com.payment.auth.dto.AuthResponse;
import com.payment.auth.dto.LoginRequest;
import com.payment.auth.dto.RegisterRequest;
import com.payment.auth.service.AuthService;
import com.payment.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Registers a new user account and returns JWT tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    /**
     * POST /api/v1/auth/login
     * Authenticates a user and returns JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Login request for username: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    /**
     * POST /api/v1/auth/refresh-token
     * Issues a new access token using a valid refresh token.
     * The refresh token is passed in the Authorization header as "Bearer {token}".
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or invalid Authorization header", "MISSING_TOKEN"));
        }
        String refreshToken = authorizationHeader.substring(7);
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }
}
