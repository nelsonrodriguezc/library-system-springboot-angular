package com.libris.book.dto;

import com.libris.book.BookStatus;

/**
 * Filters accepted by the catalogue listing.
 *
 * @param term    free text matched against title, author and ISBN
 * @param status  availability filter
 * @param subject exact subject as stored, for the "Tema" dropdown
 */
public record BookSearchQuery(String term, BookStatus status, String subject) {

    public static BookSearchQuery empty() {
        return new BookSearchQuery(null, null, null);
    }
}
