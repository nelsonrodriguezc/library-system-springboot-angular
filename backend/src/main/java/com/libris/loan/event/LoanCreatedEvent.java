package com.libris.loan.event;

/**
 * Published once the loan is committed. Carries only the id on purpose: listeners run on
 * another thread and after the transaction closed, so they must reload what they need
 * rather than hold on to a detached entity.
 */
public record LoanCreatedEvent(Long loanId) {
}
