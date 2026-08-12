package com.libris.loan.dto;

import com.libris.book.Book;
import com.libris.loan.Loan;
import com.libris.loan.LoanStatus;
import java.time.LocalDate;

/**
 * A loan as the interface needs it: the derived status and the day counts are computed
 * here so the client never has to reimplement the rules to render a badge.
 */
public record LoanResponse(
        Long id,
        BookSummary book,
        String borrowerName,
        String borrowerEmail,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status,
        long daysUntilDue,
        long daysLate) {

    public record BookSummary(Long id, String title, String author, String isbn, String coverUrl) {

        static BookSummary from(Book book) {
            return new BookSummary(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getCoverUrl());
        }
    }

    public static LoanResponse from(Loan loan, LoanStatus status, LocalDate today) {
        return new LoanResponse(
                loan.getId(),
                BookSummary.from(loan.getBook()),
                loan.getBorrowerName(),
                loan.getBorrowerEmail(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                status,
                loan.daysUntilDue(today),
                loan.daysLate(today));
    }
}
