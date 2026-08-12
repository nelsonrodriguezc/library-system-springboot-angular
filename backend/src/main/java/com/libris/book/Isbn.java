package com.libris.book;

import java.util.Locale;

/**
 * ISBNs are typed with hyphens and spaces in every possible arrangement. Normalising on
 * the way in is what makes the uniqueness rule and the Open Library lookup reliable:
 * "978-0-13-235088-4" and "9780132350884" must be the same book.
 */
public final class Isbn {

    private static final int ISBN_10_LENGTH = 10;
    private static final int ISBN_13_LENGTH = 13;

    private Isbn() {
    }

    /** Strips separators and upper-cases the ISBN-10 check character. */
    public static String normalise(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String raw) {
        String normalised = normalise(raw);
        if (normalised == null) {
            return false;
        }
        return switch (normalised.length()) {
            case ISBN_10_LENGTH -> isValidIsbn10(normalised);
            case ISBN_13_LENGTH -> isValidIsbn13(normalised);
            default -> false;
        };
    }

    private static boolean isValidIsbn10(String isbn) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            char digit = isbn.charAt(i);
            if (!Character.isDigit(digit)) {
                return false;
            }
            sum += (digit - '0') * (10 - i);
        }
        char checkCharacter = isbn.charAt(9);
        int check = checkCharacter == 'X' ? 10 : Character.isDigit(checkCharacter) ? checkCharacter - '0' : -1;
        return check >= 0 && (sum + check) % 11 == 0;
    }

    private static boolean isValidIsbn13(String isbn) {
        int sum = 0;
        for (int i = 0; i < ISBN_13_LENGTH; i++) {
            char digit = isbn.charAt(i);
            if (!Character.isDigit(digit)) {
                return false;
            }
            sum += (digit - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return sum % 10 == 0;
    }
}
