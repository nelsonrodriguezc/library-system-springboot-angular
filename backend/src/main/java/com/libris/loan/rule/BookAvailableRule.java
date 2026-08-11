package com.libris.loan.rule;

import com.libris.book.BookNotAvailableException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** The copy has to be on the shelf. */
@Component
@Order(10)
public class BookAvailableRule implements LoanRule {

    @Override
    public void check(LoanRequest request) {
        if (!request.book().isAvailable()) {
            throw new BookNotAvailableException(request.book().getTitle(), request.book().getStatus());
        }
    }
}
