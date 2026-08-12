package com.libris.loan.projection;

/** One point of the "préstamos por mes" chart. */
public interface MonthlyLoanCount {

    /** Formatted as {@code YYYY-MM}. */
    String getMonth();

    long getTotal();
}
