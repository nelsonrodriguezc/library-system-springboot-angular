package com.libris.loan.rule;

/**
 * One condition that must hold before a copy leaves the library.
 *
 * <p>Rules are discovered by Spring and applied in {@code @Order} sequence, so adding a
 * condition means adding a class: {@code LoanService} never changes. Each rule throws its
 * own business exception, which keeps the error the client sees specific to the rule that
 * actually rejected the request.
 */
public interface LoanRule {

    void check(LoanRequest request);
}
