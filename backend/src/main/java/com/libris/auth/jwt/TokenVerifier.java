package com.libris.auth.jwt;

import com.libris.auth.AuthenticatedUser;
import java.util.Optional;

/**
 * Validates an access token. Returns an empty result for anything unusable — expired,
 * tampered with, or malformed — so callers never have to catch library-specific
 * exceptions to tell "not authenticated" from "broken".
 */
public interface TokenVerifier {

    Optional<AuthenticatedUser> verify(String token);
}
