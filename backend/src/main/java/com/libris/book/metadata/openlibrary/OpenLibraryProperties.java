package com.libris.book.metadata.openlibrary;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param userAgent Open Library asks API clients to identify themselves and throttles
 *                  anonymous traffic, so this is not optional in practice.
 */
@ConfigurationProperties(prefix = "libris.open-library")
public record OpenLibraryProperties(
        String baseUrl,
        String coversUrl,
        int timeoutSeconds,
        String userAgent) {

    public OpenLibraryProperties {
        baseUrl = orDefault(baseUrl, "https://openlibrary.org");
        coversUrl = orDefault(coversUrl, "https://covers.openlibrary.org");
        userAgent = orDefault(userAgent, "Libris/1.0 (library loan management)");
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 3;
        }
    }

    public String coverByIsbn(String isbn) {
        return "%s/b/isbn/%s-L.jpg".formatted(coversUrl, isbn);
    }

    public String coverById(long coverId) {
        return "%s/b/id/%d-L.jpg".formatted(coversUrl, coverId);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
