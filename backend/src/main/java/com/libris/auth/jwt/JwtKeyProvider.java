package com.libris.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the signing key once, at startup.
 *
 * <p>When no secret is configured the application still boots with a freshly generated
 * key instead of refusing to start, so `docker compose up` stays a single command. The
 * trade-off is logged loudly: every restart invalidates the tokens issued before it.
 */
@Component
public class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);
    private static final int MINIMUM_SECRET_LENGTH = 32;

    private final SecretKey signingKey;

    public JwtKeyProvider(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            this.signingKey = Jwts.SIG.HS256.key().build();
            log.warn("""
                    No JWT_SECRET configured: an ephemeral signing key was generated. \
                    Previously issued tokens stop working after every restart. \
                    Set JWT_SECRET for anything beyond a local run.""");
        } else if (secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MINIMUM_SECRET_LENGTH + " characters long for HS256");
        } else {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public SecretKey signingKey() {
        return signingKey;
    }
}
