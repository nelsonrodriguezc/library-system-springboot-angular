package com.libris.config;

import com.libris.book.metadata.openlibrary.OpenLibraryProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Client used for the Open Library lookups.
     *
     * <p>Two details matter here. The timeouts are short and cover both connect and read,
     * because a catalogue form must not hang on a third party. And redirects are followed
     * on purpose: {@code /isbn/{isbn}.json} answers 302 towards the canonical edition URL.
     */
    @Bean
    public RestClient openLibraryRestClient(RestClient.Builder builder, OpenLibraryProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return builder
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                // Open Library throttles clients that do not identify themselves.
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }
}
