package com.libris.user.admin;

import com.libris.loan.LoanProperties;
import java.time.Instant;

/**
 * How an account looks to an administrator. Derived, never stored: ADVERTENCIA in
 * particular is just "one more late return away from a block", which is a function of the
 * configured limit.
 */
public enum UserAccountStatus {

    ACTIVO,
    ADVERTENCIA,
    BLOQUEADO;

    public static UserAccountStatus of(Instant blockedUntil, long lateReturns, Instant now, LoanProperties rules) {
        if (blockedUntil != null && blockedUntil.isAfter(now)) {
            return BLOQUEADO;
        }
        return lateReturns >= rules.overdueLimit() - 1 ? ADVERTENCIA : ACTIVO;
    }
}
