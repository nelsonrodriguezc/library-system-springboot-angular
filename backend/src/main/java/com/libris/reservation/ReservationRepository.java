package com.libris.reservation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Head of the waiting list for a title. */
    Optional<Reservation> findFirstByBookIdAndStatusOrderByRequestedAtAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndRequesterEmailIgnoreCaseAndStatus(
            Long bookId, String requesterEmail, ReservationStatus status);

    List<Reservation> findByRequesterEmailIgnoreCaseOrderByRequestedAtDesc(String requesterEmail);

    List<Reservation> findByBookIdAndStatusInOrderByRequestedAtAsc(
            Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBookIdAndRequesterEmailIgnoreCaseAndStatusIn(
            Long bookId, String requesterEmail, Collection<ReservationStatus> statuses);
}
