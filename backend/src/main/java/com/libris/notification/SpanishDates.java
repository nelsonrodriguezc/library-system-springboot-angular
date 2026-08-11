package com.libris.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Dates as a Spanish-speaking reader expects to see them in an e-mail. */
public final class SpanishDates {

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", SPANISH);

    private SpanishDates() {
    }

    public static String format(LocalDate date) {
        return date == null ? "" : LONG_DATE.format(date);
    }

    public static String format(Instant instant) {
        return instant == null ? "" : LONG_DATE.format(instant.atZone(ZoneId.systemDefault()).toLocalDate());
    }
}
