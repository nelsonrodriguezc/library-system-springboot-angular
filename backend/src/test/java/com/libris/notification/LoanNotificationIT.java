package com.libris.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.libris.testsupport.AbstractIntegrationTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Proves the notification really leaves the application, against an embedded SMTP server.
 *
 * <p>Delivery is asynchronous and fires after the transaction commits, so every assertion
 * waits for the message rather than assuming it is already there.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanNotificationIT extends AbstractIntegrationTest {

    /**
     * One SMTP server for the whole class rather than one per method: notifications are
     * delivered on another thread after the response is written, so a server torn down at
     * the end of each method would still be receiving mail as it shut down.
     */
    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    @BeforeEach
    void emptyTheMailboxes() throws FolderException {
        GREEN_MAIL.purgeEmailFromAllMailboxes();
    }

    private long anyAvailableBookId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/books")
                        .param("status", "DISPONIBLE").param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, token))
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
    @Order(1)
    @DisplayName("registrar un préstamo envía el correo de confirmación al lector")
    void sendsTheLoanConfirmation() throws Exception {
        String reader = bearer(readerToken());

        borrow(reader, anyAvailableBookId(reader));

        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 1))
                .as("el correo de confirmación debe salir sin intervención")
                .isTrue();

        MimeMessage message = GREEN_MAIL.getReceivedMessages()[0];
        assertThat(message.getSubject()).startsWith("Préstamo confirmado:");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("lector@libris.cl");
        assertThat(message.getFrom()[0].toString()).isEqualTo("biblioteca@libris.local");

        String body = GreenMailUtil.getBody(message);
        assertThat(body).contains("Fecha l");        // "Fecha límite", sin depender del encoding
        assertThat(body).contains("Libris");
    }

    @Test
    @Order(2)
    @DisplayName("el envío no bloquea la petición que lo origina")
    void deliveryDoesNotBlockTheRequest() throws Exception {
        String reader = bearer(readerToken());
        long bookId = anyAvailableBookId(reader);

        long startedAt = System.currentTimeMillis();
        borrow(reader, bookId);
        long elapsed = System.currentTimeMillis() - startedAt;

        assertThat(elapsed)
                .as("la respuesta HTTP no espera al servidor de correo")
                .isLessThan(3_000L);
        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 1)).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("devolver un ejemplar reservado avisa a quien estaba primero en la fila")
    void notifiesTheWaitingReader() throws Exception {
        String reader = bearer(readerToken());
        String librarian = bearer(librarianToken());
        long bookId = anyAvailableBookId(reader);

        JsonNode loan = borrow(reader, bookId);
        GREEN_MAIL.waitForIncomingEmail(10_000, 1);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, librarian)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d}".formatted(bookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/loans/{id}/return", loan.get("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk());

        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 2)).isTrue();

        MimeMessage waitingListMessage = GREEN_MAIL.getReceivedMessages()[1];
        assertThat(waitingListMessage.getSubject()).startsWith("Ya está disponible para ti:");
        assertThat(waitingListMessage.getAllRecipients()[0].toString()).isEqualTo("bibliotecario@libris.cl");
    }

    @Test
    @Order(4)
    @DisplayName("la tarea de recordatorios no repite un aviso ya enviado")
    void reminderSweepIsIdempotent() throws Exception {
        String admin = bearer(adminToken());

        String first = mockMvc.perform(post("/api/admin/notifications/due-soon-reminders")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(first).get("sent").asInt())
                .as("sin préstamos por vencer no hay nada que enviar")
                .isZero();

        String second = mockMvc.perform(post("/api/admin/notifications/due-soon-reminders")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(second).get("sent").asInt()).isZero();

        assertThat(GREEN_MAIL.waitForIncomingEmail(1_000, 1))
                .as("no debe salir ningún correo")
                .isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("solo un ADMIN puede disparar las tareas de correo")
    void onlyAdminsCanTriggerTheSweeps() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/due-soon-reminders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(librarianToken())))
                .andExpect(status().isForbidden());
    }

    /**
     * Runs last on purpose. It takes the SMTP server down to prove a mail outage cannot
     * cost the library a loan, and a stopped GreenMail server cannot be restarted, so
     * nothing after this one may depend on it.
     */
    @Test
    @Order(99)
    @DisplayName("una entrega fallida no rompe la operación de negocio")
    void aFailedDeliveryDoesNotBreakTheLoan() throws Exception {
        // stopService(), not Thread.stop(): the latter was removed in Java 20.
        GREEN_MAIL.getSmtp().stopService();

        String reader = bearer(readerToken());
        long bookId = anyAvailableBookId(reader);

        borrow(reader, bookId);

        mockMvc.perform(get("/api/loans/mine").header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/books/{id}", bookId).header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(jsonPath("$.status").value("PRESTADO"));
    }
}
