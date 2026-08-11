package com.libris.book.metadata;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filters the noise out of an external subject list.
 *
 * <p>Open Library mixes real subjects with shelving codes: alongside "Object-oriented
 * programming" it returns Library of Congress call numbers like "Qa76.73.j38 b57 2001"
 * and Dewey numbers like "005.13/3". Those are meaningless to a reader browsing by topic,
 * they crowd out the useful entries, and they poison the recommendations, where a shared
 * call number would count as a shared interest.
 */
public final class SubjectSanitizer {

    /** Dewey Decimal, e.g. "005.13/3" or "813.54". */
    private static final Pattern DEWEY = Pattern.compile("^\\d{1,3}([.,/]\\d+)*$");

    /** Library of Congress call number, e.g. "QA76.73.J38" or "Qa76.73.j38 b57 2001". */
    private static final Pattern LC_CALL_NUMBER = Pattern.compile("^[A-Za-z]{1,3}\\d+([.\\s].*)?$");

    private static final int MINIMUM_LENGTH = 3;
    private static final int MAXIMUM_LENGTH = 120;

    private SubjectSanitizer() {
    }

    public static List<String> clean(Collection<String> subjects, int limit) {
        if (subjects == null) {
            return List.of();
        }
        return subjects.stream()
                .filter(subject -> subject != null && !subject.isBlank())
                .map(String::trim)
                .filter(SubjectSanitizer::isMeaningful)
                .distinct()
                .limit(limit)
                .toList();
    }

    private static boolean isMeaningful(String subject) {
        if (subject.length() < MINIMUM_LENGTH || subject.length() > MAXIMUM_LENGTH) {
            return false;
        }
        if (DEWEY.matcher(subject).matches() || LC_CALL_NUMBER.matcher(subject).matches()) {
            return false;
        }
        // A "subject" made mostly of digits and punctuation is a code, not a topic.
        long letters = subject.chars().filter(Character::isLetter).count();
        return letters >= subject.length() / 2.0;
    }
}
