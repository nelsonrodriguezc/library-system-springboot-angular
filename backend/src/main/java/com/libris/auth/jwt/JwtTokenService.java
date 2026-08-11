package com.libris.auth.jwt;

import com.libris.auth.AuthenticatedUser;
import com.libris.user.AppUser;
import com.libris.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The only place that knows the token is a JWT. Everything else talks to
 * {@link TokenIssuer} / {@link TokenVerifier}.
 */
@Service
public class JwtTokenService implements TokenIssuer, TokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";

    private final JwtKeyProvider keyProvider;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtKeyProvider keyProvider, JwtProperties properties, Clock clock) {
        this.keyProvider = keyProvider;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(AppUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(keyProvider.signingKey())
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    @Override
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyProvider.signingKey())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    claims.get(CLAIM_USER_ID, Long.class),
                    claims.get(CLAIM_NAME, String.class),
                    claims.getSubject(),
                    UserRole.valueOf(claims.get(CLAIM_ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
