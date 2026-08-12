package com.libris.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory cache for the external ISBN lookups. A day of freshness is generous for data
 * that describes a printed edition, and the size cap keeps a long-running instance from
 * growing without bound.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String OPEN_LIBRARY_LOOKUP_CACHE = "openLibraryLookup";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(OPEN_LIBRARY_LOOKUP_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofHours(24))
                .recordStats());
        return cacheManager;
    }
}
