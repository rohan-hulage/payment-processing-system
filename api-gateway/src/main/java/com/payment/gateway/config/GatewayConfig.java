package com.payment.gateway.config;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.payment-service.url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Value("${services.transaction-service.url:http://localhost:8083}")
    private String transactionServiceUrl;

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
                .uri(authServiceUrl)
            )

            // Payment Service — JWT required (enforced by JwtAuthGatewayFilter)
            .route("payment-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .removeRequestHeader("Cookie")
                )
                .uri(paymentServiceUrl)
            )

            // Transaction Service — JWT required
            .route("transaction-service", r -> r
                .path("/api/v1/transactions/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .removeRequestHeader("Cookie")
                )
                .uri(transactionServiceUrl)
            )

            .build();
    }
}
