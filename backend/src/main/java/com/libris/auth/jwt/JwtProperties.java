package com.libris.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret            HS256 signing key. Deliberately empty by default: no secret is
 *                          ever committed. See {@link JwtKeyProvider} for the fallback.
 * @param expirationMinutes lifetime of an issued token.
 */
@ConfigurationProperties(prefix = "libris.security.jwt")
public record JwtProperties(String secret, int expirationMinutes) {

    public JwtProperties {
        if (expirationMinutes <= 0) {
            expirationMinutes = 480;
        }
    }
}
