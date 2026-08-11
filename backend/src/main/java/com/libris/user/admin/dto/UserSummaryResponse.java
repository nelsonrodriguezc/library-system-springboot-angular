package com.libris.user.admin.dto;

import com.libris.user.UserRole;
import com.libris.user.admin.UserAccountStatus;
import java.time.Instant;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        long activeLoans,
        long lateReturns,
        UserAccountStatus status,
        Instant blockedUntil,
        Instant createdAt) {
}
