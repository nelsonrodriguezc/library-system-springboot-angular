package com.libris.book.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Open Library stores publication dates as free text: "2008", "July 2008",
 * "August 1, 2008", "c1995". Only the year matters to us, so we pull it out and ignore
 * everything else rather than trying to parse an open-ended format.
 */
public final class PublishDateParser {

    private static final Pattern YEAR = Pattern.compile("\\b(1[0-9]{3}|20[0-9]{2}|21[0-9]{2})\\b");

    private PublishDateParser() {
    }

    public static Integer yearFrom(String publishDate) {
        if (publishDate == null || publishDate.isBlank()) {
            return null;
        }
        Matcher matcher = YEAR.matcher(publishDate);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
