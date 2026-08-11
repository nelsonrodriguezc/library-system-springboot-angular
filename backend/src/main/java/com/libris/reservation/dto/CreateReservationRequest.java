package com.libris.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(

        @NotNull(message = "El libro es obligatorio")
        Long bookId) {
}
