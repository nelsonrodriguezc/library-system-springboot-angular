package com.libris.book.metadata.openlibrary;

import static org.assertj.core.api.Assertions.assertThat;

import com.libris.book.metadata.ExternalBookData;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The external catalogue is simulated so the whole range of ways it can misbehave is
 * covered, including the one that matters most: when it fails, registering a book must
 * still work.
 */
class OpenLibraryBooksApiProviderTest {

    private static final String ISBN = "9780132350884";
    private static final int TIMEOUT_SECONDS = 1;

    private MockWebServer server;
    private OpenLibraryBooksApiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        OpenLibraryProperties properties = new OpenLibraryProperties(
                server.url("/").toString().replaceAll("/$", ""),
                "https://covers.openlibrary.org",
                TIMEOUT_SECONDS,
                "Libris/test");

        Duration timeout = Duration.ofSeconds(TIMEOUT_SECONDS);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);

        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();

        provider = new OpenLibraryBooksApiProvider(restClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private void respondWith(String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    @Test
    @DisplayName("una respuesta completa se traduce a los datos del libro")
    void mapsAFullResponse() throws InterruptedException {
        respondWith("""
                {
                  "ISBN:9780132350884": {
                    "title": "Clean Code",
                    "subtitle": "A Handbook of Agile Software Craftsmanship",
                    "authors": [{"name": "Robert C. Martin"}],
                    "publish_date": "July 2008",
                    "cover": {"large": "https://covers.openlibrary.org/b/id/15126503-L.jpg"},
                    "subjects": [{"name": "Agile software development"}, {"name": "Computer software"}]
                  }
                }
                """);

        Optional<ExternalBookData> result = provider.findByIsbn(ISBN);

        assertThat(result).isPresent();
        ExternalBookData data = result.orElseThrow();
        assertThat(data.title()).isEqualTo("Clean Code");
        assertThat(data.subtitle()).isEqualTo("A Handbook of Agile Software Craftsmanship");
        assertThat(data.author()).isEqualTo("Robert C. Martin");
        assertThat(data.publicationYear()).isEqualTo(2008);
        assertThat(data.coverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/15126503-L.jpg");
        assertThat(data.subjects()).containsExactly("Agile software development", "Computer software");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("bibkeys=ISBN:9780132350884").contains("jscmd=data");
        assertThat(request.getHeader("User-Agent")).isEqualTo("Libris/test");
    }

    @Test
    @DisplayName("un ISBN que el catálogo no conoce devuelve vacío")
    void emptyBodyMeansUnknownIsbn() {
        respondWith("{}");
        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("un registro sin datos útiles se descarta")
    void discardsARecordWithNothingUsable() {
        respondWith("{\"ISBN:9780132350884\": {}}");
        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("un 404 no propaga excepción")
    void notFoundDegradesToEmpty() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("un 500 del servicio externo no propaga excepción")
    void serverErrorDegradesToEmpty() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("un 429 por límite de uso tampoco rompe nada")
    void rateLimitDegradesToEmpty() {
        server.enqueue(new MockResponse().setResponseCode(429));
        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("un cuerpo que no es JSON válido se ignora")
    void malformedBodyDegradesToEmpty() {
        respondWith("{ esto no es json ");
        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("si el servicio no responde a tiempo, se corta y se devuelve vacío")
    void timeoutDegradesToEmpty() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
                .setBodyDelay(TIMEOUT_SECONDS * 4L, TimeUnit.SECONDS));

        long startedAt = System.currentTimeMillis();
        Optional<ExternalBookData> result = provider.findByIsbn(ISBN);
        long elapsed = System.currentTimeMillis() - startedAt;

        assertThat(result).isEmpty();
        assertThat(elapsed)
                .as("debe cortar cerca del timeout configurado, no esperar a la respuesta")
                .isLessThan(TIMEOUT_SECONDS * 3000L);
    }

    @Test
    @DisplayName("un registro parcial, sin autor, sigue siendo útil")
    void keepsAPartialRecord() {
        respondWith("""
                {"ISBN:9780132350884": {"title": "Clean Code", "publish_date": "2008"}}
                """);

        ExternalBookData data = provider.findByIsbn(ISBN).orElseThrow();
        assertThat(data.title()).isEqualTo("Clean Code");
        assertThat(data.author()).isNull();
        assertThat(data.publicationYear()).isEqualTo(2008);
        assertThat(data.coverUrl()).endsWith("/b/isbn/9780132350884-L.jpg");
    }
}
