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

    /**
     * True when the source told us nothing about this book.
     *
     * <p>The cover deliberately does not count: it is derived from the ISBN by convention
     * rather than reported by the source, so a record carrying only a cover URL is still
     * an empty answer — and treating it as a hit would stop the next source from being
     * asked at all.
     */
    public boolean isEmpty() {
        return isBlank(title) && isBlank(author) && publicationYear == null && subjects.isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
