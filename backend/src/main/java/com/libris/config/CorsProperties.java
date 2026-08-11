package com.libris.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "libris.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:4200");
        }
    }
}
