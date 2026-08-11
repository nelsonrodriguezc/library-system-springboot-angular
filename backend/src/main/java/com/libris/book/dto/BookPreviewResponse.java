package com.libris.book.dto;

import com.libris.book.metadata.ExternalBookData;
import java.util.List;

/**
 * Preview shown by the "Autocompletar desde ISBN" step before anything is saved.
 *
 * @param alreadyInCatalogue lets the wizard warn about a duplicate before the user fills
 *                           in the rest of the form, instead of failing on submit.
 */
public record BookPreviewResponse(
        String isbn,
        String title,
        String subtitle,
        String author,
        Integer publicationYear,
        String coverUrl,
        String description,
        List<String> subjects,
        boolean alreadyInCatalogue) {

    public static BookPreviewResponse from(ExternalBookData data, boolean alreadyInCatalogue) {
        return new BookPreviewResponse(
                data.isbn(),
                data.title(),
                data.subtitle(),
                data.author(),
                data.publicationYear(),
                data.coverUrl(),
                data.description(),
                data.subjects(),
                alreadyInCatalogue);
    }
}
