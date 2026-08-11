package com.libris.reservation;

import com.libris.auth.AuthenticatedUser;
import com.libris.reservation.dto.CreateReservationRequest;
import com.libris.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, caller));
    }

    /**
     * Not listed in the original endpoint table, but the "Mis reservas" screen cannot be
     * built without it. Documented as such in the README.
     */
    @GetMapping("/mine")
    public List<ReservationResponse> mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        return reservationService.findMine(caller);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser caller) {
        reservationService.cancel(id, caller);
        return ResponseEntity.noContent().build();
    }
}
