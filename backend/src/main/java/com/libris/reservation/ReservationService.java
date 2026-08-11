package com.libris.reservation;

import com.libris.auth.AuthenticatedUser;
import com.libris.book.Book;
import com.libris.book.BookRepository;
import com.libris.loan.LoanRepository;
import com.libris.reservation.ReservationExceptions.AlreadyBorrowedException;
import com.libris.reservation.ReservationExceptions.BookAlreadyAvailableException;
import com.libris.reservation.ReservationExceptions.DuplicateReservationException;
import com.libris.reservation.ReservationExceptions.ReservationAccessDeniedException;
import com.libris.reservation.ReservationExceptions.ReservationNotActiveException;
import com.libris.reservation.dto.CreateReservationRequest;
import com.libris.reservation.dto.ReservationResponse;
import com.libris.reservation.event.BookAvailableEvent;
import com.libris.shared.exception.NotFoundException;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private static final List<ReservationStatus> ACTIVE =
            List.of(ReservationStatus.PENDIENTE, ReservationStatus.NOTIFICADO);

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final LoanRepository loans;
    private final AppUserRepository users;
    private final WaitingList waitingList;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations,
                              BookRepository books,
                              LoanRepository loans,
                              AppUserRepository users,
                              WaitingList waitingList,
                              ApplicationEventPublisher events,
                              Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.loans = loans;
        this.users = users;
        this.waitingList = waitingList;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest request, AuthenticatedUser caller) {
        Book book = books.findById(request.bookId())
                .orElseThrow(() -> new NotFoundException("Libro", request.bookId()));
        AppUser requester = users.findByEmailIgnoreCase(caller.email())
                .orElseThrow(() -> new NotFoundException("Cuenta", caller.email()));

        if (book.isAvailable()) {
            throw new BookAlreadyAvailableException(book.getTitle());
        }
        if (reservations.existsByBookIdAndRequesterEmailIgnoreCaseAndStatusIn(
                book.getId(), requester.getEmail(), ACTIVE)) {
            throw new DuplicateReservationException(book.getTitle());
        }
        loans.findFirstByBookIdAndReturnDateIsNull(book.getId())
                .filter(loan -> loan.getBorrowerEmail().equalsIgnoreCase(requester.getEmail()))
                .ifPresent(loan -> {
                    throw new AlreadyBorrowedException(book.getTitle());
                });

        Reservation reservation = reservations.save(new Reservation(
                book, requester.getName(), requester.getEmail(), clock.instant()));

        return ReservationResponse.from(reservation, waitingList.positionOf(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findMine(AuthenticatedUser caller) {
        return reservations.findByRequesterEmailIgnoreCaseOrderByRequestedAtDesc(caller.email()).stream()
                .map(reservation -> ReservationResponse.from(reservation, waitingList.positionOf(reservation)))
                .toList();
    }

    /**
     * Leaves the queue. When the reader was the one holding the copy, it goes back to
     * whoever is next, or on to the shelf if nobody else is waiting.
     */
    @Transactional
    public void cancel(Long reservationId, AuthenticatedUser caller) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva", reservationId));

        if (!caller.isAdmin() && !reservation.getRequesterEmail().equalsIgnoreCase(caller.email())) {
            throw new ReservationAccessDeniedException();
        }
        if (!reservation.isActive()) {
            throw new ReservationNotActiveException(reservationId);
        }

        boolean wasHoldingTheCopy = reservation.getStatus() == ReservationStatus.NOTIFICADO;
        reservation.cancel(clock.instant());

        if (wasHoldingTheCopy) {
            passHoldToNextInLine(reservation.getBook());
        }
    }

    private void passHoldToNextInLine(Book book) {
        waitingList.nextInLineFor(book.getId()).ifPresentOrElse(
                next -> {
                    next.markNotified(clock.instant());
                    book.markReserved();
                    events.publishEvent(new BookAvailableEvent(next.getId()));
                },
                book::markAvailable);
    }
}
