package com.libris.auth.jwt;

import com.libris.user.AppUser;
import java.time.Instant;

/**
 * Issues access tokens. Segregated from {@link TokenVerifier} so that the sign-in flow
 * depends only on minting, and the request filter only on validation.
 */
public interface TokenIssuer {

    IssuedToken issue(AppUser user);

    record IssuedToken(String value, Instant expiresAt) {
    }
}
