package com.libris.reservation;

import com.libris.loan.Loan;
import com.libris.loan.LoanLifecycleListener;
import com.libris.reservation.event.BookAvailableEvent;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Connects the waiting list to the lending flow.
 *
 * <p>On return, if somebody is waiting for that exact title, the copy is held for the
 * first of them instead of going back on the shelf. On a new loan, a hold that belongs to
 * the borrower is closed off as fulfilled.
 */
@Component
public class WaitingListLoanListener implements LoanLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(WaitingListLoanListener.class);

    private final WaitingList waitingList;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public WaitingListLoanListener(WaitingList waitingList, ApplicationEventPublisher events, Clock clock) {
        this.waitingList = waitingList;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void onLoanReturned(Loan loan) {
        waitingList.nextInLineFor(loan.getBook().getId()).ifPresent(reservation -> {
            reservation.markNotified(clock.instant());
            loan.getBook().markReserved();
            log.info("Book {} held for {} who was first in the waiting list",
                    loan.getBook().getId(), reservation.getRequesterEmail());
            events.publishEvent(new BookAvailableEvent(reservation.getId()));
        });
    }

    @Override
    public void onLoanCreated(Loan loan) {
        waitingList.holderOf(loan.getBook().getId())
                .filter(reservation -> reservation.getRequesterEmail().equalsIgnoreCase(loan.getBorrowerEmail()))
                .ifPresent(reservation -> reservation.fulfil(clock.instant()));
    }
}
