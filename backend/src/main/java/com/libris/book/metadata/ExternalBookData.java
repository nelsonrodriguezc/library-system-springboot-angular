package com.libris.book.metadata;

import java.util.List;

/**
 * What an external catalogue could tell us about an ISBN. Every field except the ISBN is
 * optional: sources routinely know the title but not the author, or the cover but not the
 * subjects, and a partial answer is still more useful than none.
 */
public record ExternalBookData(
        String isbn,
        String title,
        String subtitle,
        String author,
        Integer publicationYear,
        String coverUrl,
        String description,
        List<String> subjects) {

    public ExternalBookData {
        subjects = subjects == null ? List.of() : List.copyOf(subjects);
    }

    public boolean isEmpty() {
        return isBlank(title) && isBlank(author) && publicationYear == null && isBlank(coverUrl) && subjects.isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
