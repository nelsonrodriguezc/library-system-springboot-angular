package com.libris.user;

/**
 * Roles recognised by the platform. ADMIN manages the catalogue and the accounts,
 * BIBLIOTECARIO can browse the catalogue and borrow books.
 */
public enum UserRole {

    ADMIN,
    BIBLIOTECARIO;

    public String authority() {
        return "ROLE_" + name();
    }
}
