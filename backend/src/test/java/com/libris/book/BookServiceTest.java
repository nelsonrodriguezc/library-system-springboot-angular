package com.libris.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.libris.book.dto.BookPreviewResponse;
import com.libris.book.dto.BookResponse;
import com.libris.book.dto.CreateBookRequest;
import com.libris.book.metadata.BookMetadataProvider;
import com.libris.book.metadata.ExternalBookData;
import com.libris.book.metadata.ExternalBookLookupException;
import com.libris.shared.exception.NotFoundException;
import com.libris.testsupport.TestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceTest {

    private static final String ISBN = "9780132350884";

    @Mock private BookRepository books;
    @Mock private BookMetadataProvider metadataProvider;

    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(books, metadataProvider);
        when(books.save(any(Book.class))).thenAnswer(invocation ->
                TestFixtures.withId(invocation.getArgument(0), 10L));
        when(metadataProvider.findByIsbn(anyString())).thenReturn(Optional.empty());
    }

    private ExternalBookData externalData() {
        return new ExternalBookData(ISBN, "Clean Code", "A Handbook of Agile Software Craftsmanship",
                "Robert C. Martin", 2008, "https://covers.openlibrary.org/b/isbn/" + ISBN + "-L.jpg",
                null, List.of("Software engineering", "Best practices"));
    }

    @Test
    @DisplayName("un ISBN repetido se rechaza")
    void rejectsADuplicateIsbn() {
        when(books.existsByIsbn(ISBN)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateBookRequest(
                "Clean Code", "Robert C. Martin", ISBN, 2008, null, null, null)))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining(ISBN);
        verify(books, never()).save(any());
    }

    @Test
    @DisplayName("el ISBN se normaliza antes de comprobar si ya existe")
    void normalisesTheIsbnBeforeCheckingForDuplicates() {
        when(books.existsByIsbn(ISBN)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateBookRequest(
                "Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008, null, null, null)))
                .isInstanceOf(DuplicateIsbnException.class);
        verify(books).existsByIsbn(ISBN);
    }

    @Test
    @DisplayName("un ISBN inválido se rechaza antes de tocar la base de datos")
    void rejectsAnInvalidIsbn() {
        assertThatThrownBy(() -> service.create(new CreateBookRequest(
                "Cualquiera", "Autor", "1234567890123", null, null, null, null)))
                .isInstanceOf(InvalidIsbnException.class);
        verify(books, never()).existsByIsbn(anyString());
    }

    @Test
    @DisplayName("con solo el ISBN, la ficha se completa desde el catálogo externo")
    void fillsTheRecordFromTheExternalCatalogue() {
        when(metadataProvider.findByIsbn(ISBN)).thenReturn(Optional.of(externalData()));

        BookResponse response = service.create(new CreateBookRequest(null, null, ISBN, null, null, null, null));

        assertThat(response.title()).isEqualTo("Clean Code");
        assertThat(response.author()).isEqualTo("Robert C. Martin");
        assertThat(response.publicationYear()).isEqualTo(2008);
        assertThat(response.coverUrl()).contains("covers.openlibrary.org");
        assertThat(response.subjects()).containsExactlyInAnyOrder("Software engineering", "Best practices");
        assertThat(response.status()).isEqualTo(BookStatus.DISPONIBLE);
    }

    @Test
    @DisplayName("lo que escribió la persona gana sobre lo que dice el catálogo externo")
    void manualDataWinsOverExternalData() {
        when(metadataProvider.findByIsbn(ISBN)).thenReturn(Optional.of(externalData()));

        BookResponse response = service.create(new CreateBookRequest(
                "Título corregido a mano", "Autor corregido", ISBN, 1999, null, null, List.of("Mi tema")));

        assertThat(response.title()).isEqualTo("Título corregido a mano");
        assertThat(response.author()).isEqualTo("Autor corregido");
        assertThat(response.publicationYear()).isEqualTo(1999);
        assertThat(response.subjects()).containsExactly("Mi tema");
    }

    @Test
    @DisplayName("si el catálogo externo falla, el libro se guarda igual con los datos manuales")
    void savesWithManualDataWhenTheExternalCatalogueIsDown() {
        when(metadataProvider.findByIsbn(ISBN)).thenReturn(Optional.empty());

        BookResponse response = service.create(new CreateBookRequest(
                "Escrito a mano", "Autora a mano", ISBN, 2020, null, null, null));

        assertThat(response.title()).isEqualTo("Escrito a mano");
        assertThat(response.author()).isEqualTo("Autora a mano");
        verify(books).save(any(Book.class));
    }

    @Test
    @DisplayName("si falla el catálogo externo y no hay datos manuales, se pide completarlos")
    void asksForManualDataWhenNothingCanBeResolved() {
        when(metadataProvider.findByIsbn(ISBN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateBookRequest(null, null, ISBN, null, null, null, null)))
                .isInstanceOf(IncompleteBookDataException.class)
                .hasMessageContaining("título y el autor");
        verify(books, never()).save(any());
    }

    @Test
    @DisplayName("solo se eliminan ejemplares disponibles")
    void deletesOnlyAvailableCopies() {
        Book book = TestFixtures.book("Clean Code", ISBN);
        when(books.findWithSubjectsById(10L)).thenReturn(Optional.of(book));

        service.delete(10L);
        verify(books).delete(book);

        book.markLoaned();
        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BookNotDeletableException.class)
                .hasMessageContaining("PRESTADO");

        book.markReserved();
        assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(BookNotDeletableException.class);
    }

    @Test
    @DisplayName("eliminar un libro inexistente da 404")
    void deletingAnUnknownBookIsNotFound() {
        when(books.findWithSubjectsById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("la previsualización avisa si el ISBN ya está en el catálogo")
    void previewFlagsAnIsbnAlreadyInTheCatalogue() {
        when(metadataProvider.findByIsbn(ISBN)).thenReturn(Optional.of(externalData()));
        when(books.existsByIsbn(ISBN)).thenReturn(true);

        BookPreviewResponse preview = service.preview("978-0-13-235088-4");

        assertThat(preview.alreadyInCatalogue()).isTrue();
        assertThat(preview.title()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("la previsualización sí informa el fallo del catálogo externo")
    void previewReportsAnExternalFailure() {
        when(metadataProvider.findByIsbn(ISBN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(ISBN))
                .isInstanceOf(ExternalBookLookupException.class)
                .hasMessageContaining("manualmente");
    }
}
