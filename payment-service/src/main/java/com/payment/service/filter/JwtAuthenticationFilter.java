package com.payment.service.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT authentication filter for the payment-service.
 *
 * <p>Extracts the Bearer token from the Authorization header, validates it,
 * and populates the Spring Security context with the authenticated principal.
 * The user ID (subject) is stored as the principal name so downstream services
 * can retrieve it via {@code SecurityContextHolder}.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer:payment-auth-service}")
    private String expectedIssuer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = parseClaims(token);

            String username = claims.getSubject();
            String tokenType = (String) claims.get("type");

            if (!"access".equals(tokenType)) {
                log.warn("Rejected non-access token type '{}' for path: {}", tokenType, request.getRequestURI());
                sendUnauthorized(response, "Invalid token type");
                return;
            }

            if (claims.getExpiration().before(new Date())) {
                sendUnauthorized(response, "Token expired");
                return;
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user '{}' for path: {}", username, request.getRequestURI());

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            sendUnauthorized(response, "Token expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            sendUnauthorized(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Claims parseClaims(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"success\":false,\"message\":\"" + message + "\",\"errorCode\":\"UNAUTHORIZED\"}"
        );
    }
}
