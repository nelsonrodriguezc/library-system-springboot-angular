package com.libris.notification.port;

/** Messages that concern the state of an account. */
public interface AccountNotifier {

    void sendAccountBlocked(Long userId);
}
