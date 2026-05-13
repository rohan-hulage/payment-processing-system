package com.payment.auth.service;

import com.payment.auth.dto.AuthResponse;
import com.payment.auth.dto.LoginRequest;
import com.payment.auth.dto.RegisterRequest;
import com.payment.auth.model.Role;
import com.payment.auth.model.User;
import com.payment.auth.repository.RoleRepository;
import com.payment.auth.repository.UserRepository;
import com.payment.common.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Registers a new user and returns JWT tokens.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new PaymentException(
                "Username already taken: " + request.getUsername(),
                "USERNAME_TAKEN",
                HttpStatus.CONFLICT
            );
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new PaymentException(
                "Email already registered: " + request.getEmail(),
                "EMAIL_TAKEN",
                HttpStatus.CONFLICT
            );
        }

        // The ROLE_USER row should already exist due to Flyway migrations.
        // Still, fetch-or-create to keep registration idempotent.
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user and returns JWT tokens.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new PaymentException("Invalid username or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        log.info("User logged in: {}", request.getUsername());
        return buildAuthResponse(userDetails);
    }

    /**
     * Validates a refresh token and issues a new access token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new PaymentException("Invalid token type for refresh", "INVALID_TOKEN_TYPE", HttpStatus.UNAUTHORIZED);
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new PaymentException("Refresh token is expired or invalid", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        log.info("Refreshed access token for user: {}", username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .username(userDetails.getUsername())
                .roles(userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()))
                .build();
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .username(userDetails.getUsername())
                .roles(roles)
                .build();
    }
}
