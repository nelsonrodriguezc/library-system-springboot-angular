package com.libris.reservation;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/** The ways a reservation request can be rejected, kept together for readability. */
public final class ReservationExceptions {

    private ReservationExceptions() {
    }

    /** Nothing to wait for: the copy is on the shelf and can simply be borrowed. */
    public static class BookAlreadyAvailableException extends BusinessException {

        public BookAlreadyAvailableException(String title) {
            super(ErrorType.CONFLICT, "BOOK_ALREADY_AVAILABLE",
                    "\"%s\" está disponible ahora mismo, puedes pedirlo prestado sin reservar".formatted(title));
        }
    }

    public static class DuplicateReservationException extends BusinessException {

        public DuplicateReservationException(String title) {
            super(ErrorType.CONFLICT, "DUPLICATE_RESERVATION",
                    "Ya estás en la lista de espera de \"%s\"".formatted(title));
        }
    }

    /** Reserving a book you are already holding would put you behind yourself in the queue. */
    public static class AlreadyBorrowedException extends BusinessException {

        public AlreadyBorrowedException(String title) {
            super(ErrorType.CONFLICT, "BOOK_ALREADY_BORROWED",
                    "Ya tienes \"%s\" en préstamo".formatted(title));
        }
    }

    public static class ReservationNotActiveException extends BusinessException {

        public ReservationNotActiveException(Long id) {
            super(ErrorType.CONFLICT, "RESERVATION_NOT_ACTIVE",
                    "La reserva %d ya no está activa".formatted(id));
        }
    }

    public static class ReservationAccessDeniedException extends BusinessException {

        public ReservationAccessDeniedException() {
            super(ErrorType.FORBIDDEN, "RESERVATION_ACCESS_DENIED", "Solo puedes gestionar tus propias reservas");
        }
    }
}
