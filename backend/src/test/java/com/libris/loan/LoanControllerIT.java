package com.libris.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.libris.testsupport.AbstractIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** The lending flow over HTTP, against the real schema. */
class LoanControllerIT extends AbstractIntegrationTest {

    private long anyAvailableBookId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/books")
                        .param("status", "DISPONIBLE").param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("content").get(0).get("id").asLong();
    }

    private JsonNode borrow(String token, long bookId) throws Exception {
        String body = mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(bookId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    @DisplayName("prestar y devolver deja el ejemplar de nuevo disponible")
    void borrowAndReturnRoundTrip() throws Exception {
        String reader = bearer(readerToken());
        long bookId = anyAvailableBookId(reader);

        JsonNode loan = borrow(reader, bookId);
        assertThat(loan.get("dueDate").asText())
                .isEqualTo(LocalDate.parse(loan.get("loanDate").asText()).plusDays(14).toString());
        assertThat(loan.get("status").asText()).isEqualTo("ACTIVO");

        mockMvc.perform(get("/api/books/{id}", bookId).header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(jsonPath("$.status").value("PRESTADO"));

        mockMvc.perform(put("/api/loans/{id}/return", loan.get("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEVUELTO"))
                .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));

        mockMvc.perform(get("/api/books/{id}", bookId).header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(jsonPath("$.status").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("un ejemplar ya prestado no se puede prestar otra vez")
    void cannotBorrowACopyThatIsOut() throws Exception {
        String reader = bearer(readerToken());
        String librarian = bearer(librarianToken());
        long bookId = anyAvailableBookId(reader);

        borrow(reader, bookId);

        mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, librarian)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(bookId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("no se superan los tres préstamos activos")
    void enforcesTheActiveLoanCap() throws Exception {
        String librarian = bearer(librarianToken());

        for (int i = 0; i < 3; i++) {
            borrow(librarian, anyAvailableBookId(librarian));
        }

        mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, librarian)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(anyAvailableBookId(librarian))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAX_ACTIVE_LOANS"));
    }

    @Test
    @DisplayName("mis préstamos solo devuelve los propios")
    void mineReturnsOnlyTheCallersLoans() throws Exception {
        String reader = bearer(readerToken());
        borrow(reader, anyAvailableBookId(reader));

        mockMvc.perform(get("/api/loans/mine").header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].borrowerEmail")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("lector@libris.cl"))));
    }

    @Test
    @DisplayName("quien no es ADMIN no puede prestar a nombre de otra cuenta")
    void nonAdminCannotBorrowForSomeoneElse() throws Exception {
        String reader = bearer(readerToken());

        mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, reader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "borrowerEmail": "bibliotecario@libris.cl"}
                                """.formatted(anyAvailableBookId(reader))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOAN_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("un préstamo sin libro devuelve los errores de validación")
    void validatesTheRequestBody() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(readerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.bookId").exists());
    }

    @Test
    @DisplayName("devolver un préstamo ajeno está prohibido")
    void cannotReturnSomeoneElsesLoan() throws Exception {
        String reader = bearer(readerToken());
        JsonNode loan = borrow(reader, anyAvailableBookId(reader));

        mockMvc.perform(put("/api/loans/{id}/return", loan.get("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, bearer(librarianToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOAN_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("una reserva sobre un ejemplar prestado se sirve al devolverlo")
    void returningHandsTheCopyToTheWaitingList() throws Exception {
        String reader = bearer(readerToken());
        String librarian = bearer(librarianToken());
        long bookId = anyAvailableBookId(reader);

        JsonNode loan = borrow(reader, bookId);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, librarian)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(bookId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.queuePosition").value(1));

        mockMvc.perform(put("/api/loans/{id}/return", loan.get("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/{id}", bookId).header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(jsonPath("$.status").value("RESERVADO"));

        mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, reader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(bookId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_RESERVED_FOR_ANOTHER_USER"));

        mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, librarian)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(bookId)))
                .andExpect(status().isCreated());
    }
}
