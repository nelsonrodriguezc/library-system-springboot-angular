package com.libris.book;

import com.libris.book.dto.BookResponse;
import com.libris.book.dto.BookSearchQuery;
import com.libris.book.dto.CreateBookRequest;
import com.libris.shared.exception.NotFoundException;
import com.libris.shared.web.PageResponse;
import java.util.List;
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

    public BookService(BookRepository books) {
        this.books = books;
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

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        String isbn = Isbn.normalise(request.isbn());
        if (books.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        Book book = new Book(request.title().trim(), request.author().trim(), isbn, request.publicationYear());
        book.assignCover(request.coverUrl());
        book.describe(request.description());
        book.replaceSubjects(request.subjects());

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
}
