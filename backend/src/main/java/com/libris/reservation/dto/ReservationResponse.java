package com.libris.reservation.dto;

import com.libris.book.Book;
import com.libris.reservation.Reservation;
import com.libris.reservation.ReservationStatus;
import java.time.Instant;

/**
 * @param queuePosition 1-based place in the waiting list, null once the reservation is no
 *                      longer active. This is what the "Tu posición" column shows.
 */
public record ReservationResponse(
        Long id,
        BookSummary book,
        String requesterName,
        String requesterEmail,
        Instant requestedAt,
        ReservationStatus status,
        Instant notifiedAt,
        Instant resolvedAt,
        Integer queuePosition) {

    public record BookSummary(Long id, String title, String author, String isbn, String coverUrl) {

        static BookSummary from(Book book) {
            return new BookSummary(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getCoverUrl());
        }
    }

    public static ReservationResponse from(Reservation reservation, Integer queuePosition) {
        return new ReservationResponse(
                reservation.getId(),
                BookSummary.from(reservation.getBook()),
                reservation.getRequesterName(),
                reservation.getRequesterEmail(),
                reservation.getRequestedAt(),
                reservation.getStatus(),
                reservation.getNotifiedAt(),
                reservation.getResolvedAt(),
                queuePosition);
    }
}
