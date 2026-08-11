package com.libris.loan.rule;

import com.libris.user.UserBlockedException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Accounts blocked for repeated late returns cannot take anything else out. */
@Component
@Order(20)
public class BorrowerNotBlockedRule implements LoanRule {

    @Override
    public void check(LoanRequest request) {
        if (request.borrower().isBlockedAt(request.now())) {
            throw new UserBlockedException(request.borrower().getEmail(), request.borrower().getBlockedUntil());
        }
    }
}
