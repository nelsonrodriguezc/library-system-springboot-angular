package com.libris.notification.port;

/** Messages that concern a place in the waiting list. */
public interface ReservationNotifier {

    void sendBookAvailable(Long reservationId);
}
