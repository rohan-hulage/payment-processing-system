package com.payment.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties bound from application.yml under the "jwt" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    /** Base64-encoded HMAC-SHA256 secret key (minimum 256 bits). */
    private String secret;

    /** Access token validity in milliseconds. */
    private long accessTokenExpiration = 900_000L; // 15 minutes

    /** Refresh token validity in milliseconds. */
    private long refreshTokenExpiration = 604_800_000L; // 7 days

    /** Token issuer claim value. */
    private String issuer = "payment-auth-service";
}
