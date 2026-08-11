package com.libris.reservation;

import com.libris.book.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A reader waiting for a title that is currently out. The queue is ordered by
 * {@code requestedAt}, so the first one to ask is the first one notified.
 */
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "requester_name", nullable = false, length = 120)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 180)
    private String requesterEmail;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    /** When the reservation left the queue, either cancelled or fulfilled. */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Reservation() {
        // required by JPA
    }

    public Reservation(Book book, String requesterName, String requesterEmail, Instant requestedAt) {
        this.book = book;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requestedAt = requestedAt;
        this.status = ReservationStatus.PENDIENTE;
    }

    public boolean isActive() {
        return status.isActive();
    }

    public void markNotified(Instant moment) {
        this.status = ReservationStatus.NOTIFICADO;
        this.notifiedAt = moment;
    }

    public void cancel(Instant moment) {
        this.status = ReservationStatus.CANCELADO;
        this.resolvedAt = moment;
    }

    /** The reader picked the book up: the reservation leaves the queue for good. */
    public void fulfil(Instant moment) {
        this.status = ReservationStatus.CUMPLIDO;
        this.resolvedAt = moment;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
