package com.libris.book.metadata.openlibrary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response of {@code /isbn/{isbn}.json}, the endpoint named in the specification. It
 * carries the edition itself, so there is no author name and no subject list here: both
 * live on the related work and would each cost another round trip.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EditionRecord(
        String title,
        String subtitle,
        @JsonProperty("publish_date") String publishDate,
        List<Long> covers,
        List<String> publishers) {
}
