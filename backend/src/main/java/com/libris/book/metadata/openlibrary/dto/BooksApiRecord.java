package com.libris.book.metadata.openlibrary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One entry of {@code /api/books?bibkeys=ISBN:...&format=json&jscmd=data}. This endpoint
 * resolves author and subject names for us, which the raw edition record does not.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BooksApiRecord(
        String title,
        String subtitle,
        List<NamedEntry> authors,
        List<NamedEntry> subjects,
        @JsonProperty("publish_date") String publishDate,
        Cover cover) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedEntry(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(String small, String medium, String large) {
    }
}
