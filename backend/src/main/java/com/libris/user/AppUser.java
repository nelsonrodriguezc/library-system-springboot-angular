package com.libris.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A platform account. Borrowers are matched to accounts by e-mail, so the overdue
 * block always applies to the account that actually took the books out.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** When the temporary borrowing block expires. Null means the account is not blocked. */
    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() {
        // required by JPA
    }

    public AppUser(String name, String email, String passwordHash, UserRole role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PrePersist
    void assignCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** The block is temporary: it lapses on its own once {@code blockedUntil} is in the past. */
    public boolean isBlockedAt(Instant moment) {
        return blockedUntil != null && blockedUntil.isAfter(moment);
    }

    public void blockUntil(Instant until) {
        this.blockedUntil = until;
    }

    /** Used by an ADMIN to lift the block before it expires on its own. */
    public void liftBlock() {
        this.blockedUntil = null;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
