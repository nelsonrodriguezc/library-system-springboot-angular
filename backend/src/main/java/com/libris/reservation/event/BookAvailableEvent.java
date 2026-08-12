package com.libris.reservation.event;

/** A held copy is ready for the reader that was first in the waiting list. */
public record BookAvailableEvent(Long reservationId) {
}
