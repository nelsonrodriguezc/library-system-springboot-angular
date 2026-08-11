package com.libris.book;

import com.libris.book.dto.BookSearchQuery;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Translates the catalogue filters into criteria. Kept apart from {@code BookService} so
 * the service stays about behaviour and this stays about querying.
 */
public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> matching(BookSearchQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(query.term())) {
                String pattern = "%" + query.term().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("author")), pattern),
                        builder.like(builder.lower(root.get("isbn")), pattern)));
            }

            if (query.status() != null) {
                predicates.add(builder.equal(root.get("status"), query.status()));
            }

            if (hasText(query.subject())) {
                // Joining a collection multiplies rows, which would break the page counts.
                if (criteriaQuery != null) {
                    criteriaQuery.distinct(true);
                }
                predicates.add(builder.equal(root.join("subjects"), query.subject().trim()));
            }

            return predicates.isEmpty() ? null : builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
