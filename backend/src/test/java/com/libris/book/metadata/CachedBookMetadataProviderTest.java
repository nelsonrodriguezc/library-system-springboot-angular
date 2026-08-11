package com.libris.book.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Delegation order across sources. The caching itself is exercised by the integration test. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CachedBookMetadataProviderTest {

    private static final String ISBN = "9780132350884";

    @Mock private BookMetadataSource primary;
    @Mock private BookMetadataSource fallback;

    private ExternalBookData data(String title) {
        return new ExternalBookData(ISBN, title, null, "Autora", 2020, null, null, List.of());
    }

    @Test
    @DisplayName("se queda con la primera fuente que responde y no consulta las demás")
    void stopsAtTheFirstSourceThatAnswers() {
        when(primary.findByIsbn(ISBN)).thenReturn(Optional.of(data("Desde la fuente principal")));
        CachedBookMetadataProvider provider = new CachedBookMetadataProvider(List.of(primary, fallback));

        assertThat(provider.findByIsbn(ISBN).orElseThrow().title()).isEqualTo("Desde la fuente principal");
        verify(fallback, never()).findByIsbn(ISBN);
    }

    @Test
    @DisplayName("si la principal no sabe nada, se recurre a la siguiente")
    void fallsThroughToTheNextSource() {
        when(primary.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(fallback.findByIsbn(ISBN)).thenReturn(Optional.of(data("Desde el respaldo")));
        CachedBookMetadataProvider provider = new CachedBookMetadataProvider(List.of(primary, fallback));

        assertThat(provider.findByIsbn(ISBN).orElseThrow().title()).isEqualTo("Desde el respaldo");
        verify(primary).findByIsbn(ISBN);
    }

    @Test
    @DisplayName("si ninguna fuente responde, el resultado es vacío y no una excepción")
    void returnsEmptyWhenEverySourceFails() {
        when(primary.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(fallback.findByIsbn(ISBN)).thenReturn(Optional.empty());
        CachedBookMetadataProvider provider = new CachedBookMetadataProvider(List.of(primary, fallback));

        assertThat(provider.findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("sin fuentes configuradas tampoco falla")
    void toleratesHavingNoSources() {
        assertThat(new CachedBookMetadataProvider(List.of()).findByIsbn(ISBN)).isEmpty();
    }
}
