package com.libris.user.admin;

import com.libris.user.AppUser;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters for the account listing.
 *
 * <p>Built as criteria rather than a JPQL query with {@code :param is null} guards on
 * purpose: passing a null parameter into that pattern makes PostgreSQL infer {@code bytea}
 * for the placeholder and reject {@code lower(?)}. Criteria simply leave the predicate out.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<AppUser> matching(String search, Boolean blocked, Instant now) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern)));
            }

            if (blocked != null) {
                Predicate isBlocked = builder.and(
                        builder.isNotNull(root.get("blockedUntil")),
                        builder.greaterThan(root.get("blockedUntil"), now));
                predicates.add(blocked ? isBlocked : builder.not(isBlocked));
            }

            return predicates.isEmpty() ? null : builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
