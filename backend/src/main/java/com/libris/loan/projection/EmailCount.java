package com.libris.loan.projection;

/** Aggregate row keyed by borrower e-mail, used to count a whole page in one query. */
public interface EmailCount {

    String getEmail();

    long getTotal();
}
