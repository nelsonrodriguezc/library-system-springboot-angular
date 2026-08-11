package com.libris.auth.dto;

import com.libris.user.AppUser;
import com.libris.user.UserRole;
import java.time.Instant;

/**
 * Everything the client needs after a successful sign-in: the bearer token and the
 * profile behind it, so the interface can decide what to show without decoding the JWT.
 */
public record AuthResponse(String token, Instant expiresAt, AuthenticatedProfile user) {

    public record AuthenticatedProfile(Long id, String name, String email, UserRole role, Instant blockedUntil) {

        public static AuthenticatedProfile from(AppUser user) {
            return new AuthenticatedProfile(
                    user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getBlockedUntil());
        }
    }
}
