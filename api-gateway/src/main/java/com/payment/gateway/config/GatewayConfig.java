package com.payment.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Programmatic route configuration for the API Gateway.
 *
 * <p>Routes are also defined in application.yml for clarity, but this class
 * demonstrates how to add custom predicates and filters in code.
 * The YAML routes take precedence; this bean adds supplementary routes.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

            // Auth Service — public endpoints, no JWT required
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .removeRequestHeader("Cookie")
                )
                .uri("${services.auth-service.url:http://localhost:8081}")
            )

            // Payment Service — JWT required (enforced by JwtAuthGatewayFilter)
            .route("payment-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .removeRequestHeader("Cookie")
                    .circuitBreaker(config -> config
                        .setName("payment-service-cb")
                        .setFallbackUri("forward:/fallback/payment")
                    )
                )
                .uri("${services.payment-service.url:http://localhost:8082}")
            )

            // Transaction Service — JWT required
            .route("transaction-service", r -> r
                .path("/api/v1/transactions/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .removeRequestHeader("Cookie")
                    .circuitBreaker(config -> config
                        .setName("transaction-service-cb")
                        .setFallbackUri("forward:/fallback/transaction")
                    )
                )
                .uri("${services.transaction-service.url:http://localhost:8083}")
            )

            .build();
    }
}
