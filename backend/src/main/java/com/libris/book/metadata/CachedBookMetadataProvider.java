package com.libris.book.metadata;

import com.libris.config.CacheConfig;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Asks every configured source in order and keeps the first usable answer.
 *
 * <p>Adding another catalogue means adding a {@link BookMetadataSource} bean — nothing
 * here changes. Results are cached because what an ISBN maps to essentially never
 * changes, so going back to the network for the same ISBN is pure waste, and Open Library
 * throttles clients that do it.
 */
@Component
public class CachedBookMetadataProvider implements BookMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(CachedBookMetadataProvider.class);

    private final List<BookMetadataSource> sources;

    public CachedBookMetadataProvider(List<BookMetadataSource> sources) {
        this.sources = List.copyOf(sources);
    }

    @Override
    @Cacheable(
            cacheNames = CacheConfig.OPEN_LIBRARY_LOOKUP_CACHE,
            key = "#isbn",
            // Do not memoise a failure: the source may simply have been unreachable.
            unless = "#result == null || #result.isEmpty()")
    public Optional<ExternalBookData> findByIsbn(String isbn) {
        for (BookMetadataSource source : sources) {
            Optional<ExternalBookData> data = source.findByIsbn(isbn);
            if (data.isPresent()) {
                log.debug("ISBN {} resolved by {}", isbn, source.sourceName());
                return data;
            }
        }
        log.info("No external source could resolve ISBN {}", isbn);
        return Optional.empty();
    }
}
