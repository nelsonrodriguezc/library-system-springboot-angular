package com.libris.book;

import com.libris.book.dto.BookPreviewResponse;
import com.libris.book.dto.BookResponse;
import com.libris.book.dto.BookSearchQuery;
import com.libris.book.dto.CreateBookRequest;
import com.libris.book.metadata.BookMetadataProvider;
import com.libris.book.metadata.ExternalBookData;
import com.libris.book.metadata.ExternalBookLookupException;
import com.libris.shared.exception.NotFoundException;
import com.libris.shared.web.PageResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue use cases. Availability transitions are driven by the loan and reservation
 * services; this one owns the records themselves.
 */
@Service
public class BookService {

    private final BookRepository books;
    private final BookMetadataProvider metadataProvider;

    public BookService(BookRepository books, BookMetadataProvider metadataProvider) {
        this.books = books;
        this.metadataProvider = metadataProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(BookSearchQuery query, Pageable pageable) {
        return PageResponse.from(books.findAll(BookSpecifications.matching(query), pageable), BookResponse::from);
    }

    @Transactional(readOnly = true)
    public BookResponse findById(Long id) {
        return BookResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public List<String> catalogueSubjects() {
        return books.findDistinctSubjects();
    }

    /**
     * Shows what the external catalogue knows about an ISBN without saving anything.
     *
     * <p>Unlike {@link #create}, this one does report a failure: the caller asked for
     * external data explicitly, so silently returning an empty form would be confusing.
     */
    @Transactional(readOnly = true)
    public BookPreviewResponse preview(String rawIsbn) {
        String isbn = requireValidIsbn(rawIsbn);
        ExternalBookData data = metadataProvider.findByIsbn(isbn)
                .orElseThrow(() -> new ExternalBookLookupException(isbn));
        return BookPreviewResponse.from(data, books.existsByIsbn(isbn));
    }

    /**
     * Registers a copy. The ISBN is enough on its own: whatever the librarian did not
     * type is filled in from the external catalogue, and if that catalogue is unreachable
     * the record is still saved with the manual data.
     */
    @Transactional
    public BookResponse create(CreateBookRequest request) {
        String isbn = requireValidIsbn(request.isbn());
        if (books.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        Optional<ExternalBookData> external = metadataProvider.findByIsbn(isbn);

        String title = preferManual(request.title(), external.map(ExternalBookData::title).orElse(null));
        String author = preferManual(request.author(), external.map(ExternalBookData::author).orElse(null));
        if (title == null || author == null) {
            throw new IncompleteBookDataException(isbn);
        }

        Integer year = request.publicationYear() != null
                ? request.publicationYear()
                : external.map(ExternalBookData::publicationYear).orElse(null);

        Book book = new Book(title, author, isbn, year);
        book.assignCover(preferManual(request.coverUrl(), external.map(ExternalBookData::coverUrl).orElse(null)));
        book.describe(preferManual(request.description(), external.map(ExternalBookData::description).orElse(null)));
        book.replaceSubjects(hasEntries(request.subjects())
                ? request.subjects()
                : external.map(ExternalBookData::subjects).orElse(List.of()));

        return BookResponse.from(books.save(book));
    }

    /** Only a copy sitting on the shelf can be removed from the catalogue. */
    @Transactional
    public void delete(Long id) {
        Book book = require(id);
        if (!book.isAvailable()) {
            throw new BookNotDeletableException(book.getTitle(), book.getStatus());
        }
        books.delete(book);
    }

    private Book require(Long id) {
        return books.findWithSubjectsById(id).orElseThrow(() -> new NotFoundException("Libro", id));
    }

    private String requireValidIsbn(String rawIsbn) {
        if (!Isbn.isValid(rawIsbn)) {
            throw new InvalidIsbnException(rawIsbn);
        }
        return Isbn.normalise(rawIsbn);
    }

    /** What a person typed always beats what the external catalogue guessed. */
    private String preferManual(String manual, String external) {
        if (manual != null && !manual.isBlank()) {
            return manual.trim();
        }
        return external != null && !external.isBlank() ? external.trim() : null;
    }

    private boolean hasEntries(List<String> values) {
        return values != null && !values.isEmpty();
    }
}
