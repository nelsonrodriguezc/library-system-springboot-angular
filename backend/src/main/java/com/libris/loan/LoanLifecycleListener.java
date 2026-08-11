package com.libris.loan;

/**
 * Extension point for behaviour that other modules need to attach to a loan changing
 * hands, evaluated inside the same transaction.
 *
 * <p>This is how the waiting list plugs itself in without {@code LoanService} knowing
 * that reservations exist: the reservation module contributes an implementation, and the
 * loan module keeps working on its own if none is present.
 *
 * <p>Both methods default to doing nothing so an implementer only overrides what it
 * actually cares about.
 */
public interface LoanLifecycleListener {

    default void onLoanCreated(Loan loan) {
    }

    /**
     * Called after the copy has already been put back on the shelf, so an implementation
     * only needs to act when it wants a different outcome — for example holding it for
     * whoever was first in the queue.
     */
    default void onLoanReturned(Loan loan) {
    }
}
