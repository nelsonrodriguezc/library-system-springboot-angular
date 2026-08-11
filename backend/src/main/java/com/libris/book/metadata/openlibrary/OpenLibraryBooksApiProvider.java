package com.libris.book.metadata.openlibrary;

import com.libris.book.metadata.BookMetadataSource;
import com.libris.book.metadata.ExternalBookData;
import com.libris.book.metadata.PublishDateParser;
import com.libris.book.metadata.SubjectSanitizer;
import com.libris.book.metadata.openlibrary.dto.BooksApiRecord;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Primary source: the read-oriented Books API, which answers title, author names,
 * publication date, cover and subjects in a single request.
 *
 * <p>The specification illustrates the integration with {@code /isbn/{isbn}.json}. That
 * endpoint returns the edition only, without author names or subjects, so filling the
 * form from it would take three or four chained calls. Both are implemented behind the
 * same port: this one first, {@link OpenLibraryEditionProvider} as the fallback.
 *
 * <p>See {@link BookMetadataSource} for why nothing escapes this class as an exception.
 */
@Component
@Order(1)
public class OpenLibraryBooksApiProvider implements BookMetadataSource {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryBooksApiProvider.class);
    private static final ParameterizedTypeReference<Map<String, BooksApiRecord>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final int MAX_SUBJECTS = 10;

    private final RestClient restClient;
    private final OpenLibraryProperties properties;

    public OpenLibraryBooksApiProvider(RestClient openLibraryRestClient, OpenLibraryProperties properties) {
        this.restClient = openLibraryRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<ExternalBookData> findByIsbn(String isbn) {
        String bibKey = "ISBN:" + isbn;
        try {
            Map<String, BooksApiRecord> response = restClient.get()
                    .uri(builder -> builder.path("/api/books")
                            .queryParam("bibkeys", bibKey)
                            .queryParam("format", "json")
                            .queryParam("jscmd", "data")
                            .build())
                    .retrieve()
                    .body(RESPONSE_TYPE);

            if (response == null || !response.containsKey(bibKey)) {
                return Optional.empty();
            }
            return Optional.of(toExternalData(isbn, response.get(bibKey))).filter(data -> !data.isEmpty());
        } catch (Exception e) {
            // Timeouts, 5xx, rate limiting, malformed payloads: the catalogue carries on
            // with whatever the user typed. See BookMetadataProvider for the contract.
            log.warn("Open Library books API lookup failed for ISBN {}: {}", isbn, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public String sourceName() {
        return "Open Library Books API";
    }

    private ExternalBookData toExternalData(String isbn, BooksApiRecord record) {
        return new ExternalBookData(
                isbn,
                record.title(),
                record.subtitle(),
                firstAuthorName(record.authors()),
                PublishDateParser.yearFrom(record.publishDate()),
                coverUrl(isbn, record.cover()),
                null,
                subjectNames(record.subjects()));
    }

    private String firstAuthorName(List<BooksApiRecord.NamedEntry> authors) {
        if (authors == null || authors.isEmpty()) {
            return null;
        }
        return authors.stream()
                .map(BooksApiRecord.NamedEntry::name)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<String> subjectNames(List<BooksApiRecord.NamedEntry> subjects) {
        if (subjects == null) {
            return List.of();
        }
        // Shelving codes travel with the real subjects; see SubjectSanitizer.
        return SubjectSanitizer.clean(
                subjects.stream().map(BooksApiRecord.NamedEntry::name).toList(), MAX_SUBJECTS);
    }

    private String coverUrl(String isbn, BooksApiRecord.Cover cover) {
        if (cover != null && cover.large() != null && !cover.large().isBlank()) {
            return cover.large();
        }
        return properties.coverByIsbn(isbn);
    }
}
