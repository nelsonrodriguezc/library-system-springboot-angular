package com.libris.book.metadata.openlibrary;

import com.libris.book.metadata.BookMetadataSource;
import com.libris.book.metadata.ExternalBookData;
import com.libris.book.metadata.PublishDateParser;
import com.libris.book.metadata.openlibrary.dto.EditionRecord;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fallback source, using the {@code /isbn/{isbn}.json} endpoint named in the
 * specification. It answers a partial record — no author, no subjects — which is still
 * enough to save the person typing the title, the year and finding a cover.
 *
 * <p>Note that this endpoint answers 302 towards the canonical edition URL, so the HTTP
 * client is configured to follow redirects.
 */
@Component
@Order(2)
public class OpenLibraryEditionProvider implements BookMetadataSource {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryEditionProvider.class);

    private final RestClient restClient;
    private final OpenLibraryProperties properties;

    public OpenLibraryEditionProvider(RestClient openLibraryRestClient, OpenLibraryProperties properties) {
        this.restClient = openLibraryRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<ExternalBookData> findByIsbn(String isbn) {
        try {
            EditionRecord edition = restClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .body(EditionRecord.class);

            if (edition == null) {
                return Optional.empty();
            }
            return Optional.of(new ExternalBookData(
                            isbn,
                            edition.title(),
                            edition.subtitle(),
                            null,
                            PublishDateParser.yearFrom(edition.publishDate()),
                            coverUrl(isbn, edition.covers()),
                            null,
                            List.of()))
                    .filter(data -> !data.isEmpty());
        } catch (Exception e) {
            log.warn("Open Library edition lookup failed for ISBN {}: {}", isbn, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public String sourceName() {
        return "Open Library editions";
    }

    private String coverUrl(String isbn, List<Long> covers) {
        if (covers != null && !covers.isEmpty() && covers.get(0) != null && covers.get(0) > 0) {
            return properties.coverById(covers.get(0));
        }
        return properties.coverByIsbn(isbn);
    }
}
