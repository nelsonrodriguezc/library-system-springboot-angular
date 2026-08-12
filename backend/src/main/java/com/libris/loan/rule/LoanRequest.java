package com.libris.loan.rule;

import com.libris.book.Book;
import com.libris.user.AppUser;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Everything a {@link LoanRule} is allowed to look at. Passing a context object rather
 * than a growing parameter list means a new rule that needs one more fact does not change
 * the signature every other rule already implements.
 */
public record LoanRequest(Book book, AppUser borrower, LocalDate today, Instant now) {
}
