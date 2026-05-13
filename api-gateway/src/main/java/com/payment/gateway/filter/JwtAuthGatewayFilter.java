package com.payment.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Global JWT authentication filter for the API Gateway.
 *
 * <p>Validates the Bearer token on every request except public paths.
 * On success, forwards the authenticated username and roles as downstream headers
 * so microservices can trust the identity without re-validating the token.
 */
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Paths that bypass JWT validation. */
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh-token",
        "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public int getOrder() {
        return -100; // Run before routing filters
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip JWT validation for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseClaims(token);

            if (claims.getExpiration().before(new Date())) {
                return sendUnauthorized(exchange, "Token expired");
            }

            String tokenType = (String) claims.get("type");
            if (!"access".equals(tokenType)) {
                return sendUnauthorized(exchange, "Invalid token type");
            }

            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            String rolesHeader = roles != null ? String.join(",", roles) : "";

            // Forward identity headers to downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Authenticated-User", username)
                    .header("X-User-Roles", rolesHeader)
                    .build();

            log.debug("JWT validated for user '{}' on path '{}'", username, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT on path '{}': {}", path, e.getMessage());
            return sendUnauthorized(exchange, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT on path '{}': {}", path, e.getMessage());
            return sendUnauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
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

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
            "{\"success\":false,\"message\":\"%s\",\"errorCode\":\"UNAUTHORIZED\"}",
            message
        );
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
