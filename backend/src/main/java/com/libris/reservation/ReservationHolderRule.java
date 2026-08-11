package com.libris.reservation;

import com.libris.book.BookNotAvailableException;
import com.libris.loan.rule.LoanRequest;
import com.libris.loan.rule.LoanRule;
import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A held copy may only be taken by the reader it is being held for.
 *
 * <p>Note where this class lives: the reservation module contributes a rule to the
 * lending flow through {@link LoanRule}, so adding the waiting list required no change to
 * {@code LoanService} or to any existing rule.
 */
@Component
@Order(15)
public class ReservationHolderRule implements LoanRule {

    private final WaitingList waitingList;

    public ReservationHolderRule(WaitingList waitingList) {
        this.waitingList = waitingList;
    }

    @Override
    public void check(LoanRequest request) {
        if (!request.book().isReserved()) {
            return;
        }
        boolean heldForBorrower = waitingList.holderOf(request.book().getId())
                .map(reservation -> reservation.getRequesterEmail().equalsIgnoreCase(request.borrower().getEmail()))
                .orElse(false);

        if (!heldForBorrower) {
            throw new ReservedForSomeoneElseException(request.book().getTitle());
        }
    }

    /** Distinct from {@link BookNotAvailableException} so the interface can explain why. */
    static class ReservedForSomeoneElseException extends BusinessException {

        ReservedForSomeoneElseException(String title) {
            super(ErrorType.CONFLICT, "BOOK_RESERVED_FOR_ANOTHER_USER",
                    "El libro \"%s\" está reservado para otro lector de la lista de espera".formatted(title));
        }
    }
}
