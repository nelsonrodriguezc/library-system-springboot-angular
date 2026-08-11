package com.libris.auth;

import com.libris.user.UserRole;

/**
 * The caller behind the current request, rebuilt from the JWT claims so that no database
 * round trip is needed to authorise a request.
 */
public record AuthenticatedUser(Long id, String name, String email, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
