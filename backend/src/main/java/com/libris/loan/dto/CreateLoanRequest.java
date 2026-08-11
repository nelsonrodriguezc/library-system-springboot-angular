package com.libris.loan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * @param borrowerEmail optional. Left out, the loan is for whoever is signed in. An ADMIN
 *                      may register a loan on behalf of another account, which is what a
 *                      librarian does at the desk; anyone else is restricted to their own.
 */
public record CreateLoanRequest(

        @NotNull(message = "El libro es obligatorio")
        Long bookId,

        @Email(message = "El correo del lector no tiene un formato válido")
        String borrowerEmail) {
}
