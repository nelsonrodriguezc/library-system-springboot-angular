package com.libris.reservation;

/**
 * Lifecycle of a place in the waiting list.
 *
 * <p>PENDIENTE: waiting in the queue. NOTIFICADO: the book came back and this reader was
 * told it is being held. CUMPLIDO: the reader actually borrowed it. CANCELADO: given up.
 */
public enum ReservationStatus {

    PENDIENTE,
    NOTIFICADO,
    CANCELADO,
    CUMPLIDO;

    /** Still occupying a spot in the queue. */
    public boolean isActive() {
        return this == PENDIENTE || this == NOTIFICADO;
    }
}
