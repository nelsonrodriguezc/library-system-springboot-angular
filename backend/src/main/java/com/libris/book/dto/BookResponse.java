package com.libris.book.dto;

import com.libris.book.Book;
import com.libris.book.BookStatus;
import java.util.List;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        Integer publicationYear,
        BookStatus status,
        String coverUrl,
        String description,
        List<String> subjects) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublicationYear(),
                book.getStatus(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getSubjects().stream().sorted().toList());
    }
}
