package com.libris.book.metadata;

import java.util.Optional;

/**
 * One external catalogue that can be asked about an ISBN.
 *
 * <p>The contract is deliberately total: an implementation <strong>never</strong> throws,
 * whatever happens on the wire. A source that is down, slow, rate limited or simply does
 * not know the ISBN all produce the same empty result. That is what lets the catalogue
 * keep working when a third party does not, and it means any source can be substituted
 * without its callers changing how they handle failure.
 *
 * <p>Implementations are consulted in {@code @Order} sequence by
 * {@link CachedBookMetadataProvider}; adding a new one requires no change anywhere else.
 */
public interface BookMetadataSource {

    Optional<ExternalBookData> findByIsbn(String isbn);

    /** Human-readable name, used for logging and diagnostics. */
    String sourceName();
}
