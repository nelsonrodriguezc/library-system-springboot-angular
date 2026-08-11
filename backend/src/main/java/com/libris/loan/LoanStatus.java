package com.libris.loan;

/**
 * Presentation status of a loan, derived rather than stored: it is a function of the due
 * date and today, so persisting it would only create a value that goes stale overnight.
 *
 * <p>The four values are mutually exclusive, which is what lets the dashboard chart add
 * up to 100%.
 */
public enum LoanStatus {

    /** Out, with time to spare. */
    ACTIVO,
    /** Out, due within the "due soon" window. */
    POR_VENCER,
    /** Out and past the due date. */
    VENCIDO,
    /** Back on the shelf. */
    DEVUELTO
}
