package com.libris.book.metadata;

import java.util.Optional;

/**
 * What the application depends on when it wants to enrich a book from its ISBN.
 *
 * <p>Kept separate from {@link BookMetadataSource} on purpose: callers should not know
 * how many catalogues are consulted, in what order, or whether the answer was cached.
 * Like the sources it delegates to, this never throws.
 */
public interface BookMetadataProvider {

    Optional<ExternalBookData> findByIsbn(String isbn);
}
