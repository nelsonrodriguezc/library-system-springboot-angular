package com.libris.user;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** The account accumulated too many late returns and cannot borrow until the block lapses. */
public class UserBlockedException extends BusinessException {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public UserBlockedException(String email, Instant blockedUntil) {
        super(ErrorType.CONFLICT, "USER_BLOCKED",
                "La cuenta %s está bloqueada para pedir préstamos hasta el %s".formatted(
                        email, DAY.format(blockedUntil.atZone(ZoneId.systemDefault()))));
    }
}
