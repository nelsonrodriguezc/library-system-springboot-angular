package com.libris.reservation;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The queue of readers waiting for a title, ordered by when they asked. Owns the two
 * questions the rest of the application asks about it: who is next, and where am I.
 */
@Component
public class WaitingList {

    private static final List<ReservationStatus> ACTIVE =
            List.of(ReservationStatus.PENDIENTE, ReservationStatus.NOTIFICADO);

    private final ReservationRepository reservations;

    public WaitingList(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    /** First reader still waiting to be told the copy is back. */
    @Transactional(readOnly = true)
    public Optional<Reservation> nextInLineFor(Long bookId) {
        return reservations.findFirstByBookIdAndStatusOrderByRequestedAtAsc(bookId, ReservationStatus.PENDIENTE);
    }

    /** The reservation currently holding the copy, if any. */
    @Transactional(readOnly = true)
    public Optional<Reservation> holderOf(Long bookId) {
        return reservations.findFirstByBookIdAndStatusOrderByRequestedAtAsc(bookId, ReservationStatus.NOTIFICADO);
    }

    /**
     * 1-based place in the queue. A reader who has already been notified is holding the
     * copy, so they are always first.
     */
    @Transactional(readOnly = true)
    public Integer positionOf(Reservation reservation) {
        if (!reservation.isActive()) {
            return null;
        }
        if (reservation.getStatus() == ReservationStatus.NOTIFICADO) {
            return 1;
        }
        List<Reservation> queue = reservations.findByBookIdAndStatusInOrderByRequestedAtAsc(
                reservation.getBook().getId(), ACTIVE);
        for (int index = 0; index < queue.size(); index++) {
            if (queue.get(index).getId().equals(reservation.getId())) {
                return index + 1;
            }
        }
        return null;
    }
}
