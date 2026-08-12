package com.libris.loan.rule;

import com.libris.book.BookNotAvailableException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The copy must not be out with somebody else.
 *
 * <p>A reserved copy passes this rule: it is physically on the shelf, and whether this
 * particular reader may take it is decided by the rule that owns the waiting list.
 */
@Component
@Order(10)
public class BookAvailableRule implements LoanRule {

    @Override
    public void check(LoanRequest request) {
        if (!request.book().isAvailable() && !request.book().isReserved()) {
            throw new BookNotAvailableException(request.book().getTitle(), request.book().getStatus());
        }
    }
}
