package com.libris.book;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.libris.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class BookControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("el catálogo exige autenticación")
    void catalogueRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/books"));
    }

    @Test
    @DisplayName("cualquier cuenta autenticada puede consultar el catálogo")
    void anySignedInAccountCanBrowse() throws Exception {
        mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, bearer(librarianToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.content.length()").value(12))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    @DisplayName("la búsqueda filtra por título, autor e ISBN")
    void searchesAcrossTitleAuthorAndIsbn() throws Exception {
        String token = bearer(librarianToken());

        mockMvc.perform(get("/api/books").param("search", "clean").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/books").param("search", "fowler").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/books").param("search", "9780132350884").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("un valor de estado inválido devuelve 400 con la forma de error habitual")
    void rejectsAnUnknownStatusFilter() throws Exception {
        mockMvc.perform(get("/api/books").param("status", "NO_EXISTE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(librarianToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("un BIBLIOTECARIO no puede registrar libros")
    void librarianCannotCreateBooks() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearer(librarianToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Nuevo", "author": "Autora", "isbn": "9781617294945"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("un ADMIN registra un libro y luego puede eliminarlo")
    void adminCreatesAndDeletesABook() throws Exception {
        String admin = bearer(adminToken());

        String created = mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Spring in Action", "author": "Craig Walls",
                                 "isbn": "9781617294945", "publicationYear": 2018}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DISPONIBLE"))
                .andExpect(jsonPath("$.title").value("Spring in Action"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(delete("/api/books/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("un ISBN repetido devuelve 409 con el código de la regla")
    void duplicateIsbnIsRejected() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Otro Clean Code", "author": "Alguien", "isbn": "978-0-13-235088-4"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ISBN"));
    }

    @Test
    @DisplayName("la validación devuelve los errores por campo")
    void validationErrorsAreReportedPerField() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Sin ISBN válido", "author": "Autora", "isbn": "1234567890123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.isbn").exists());
    }

    @Test
    @DisplayName("si el catálogo externo no responde, el alta manual sigue funcionando")
    void createsWithoutTheExternalCatalogue() throws Exception {
        // The test profile points Open Library at an unroutable port on purpose.
        mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Alta manual", "author": "Autora Manual", "isbn": "9780451524935"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Alta manual"));
    }

    @Test
    @DisplayName("la previsualización informa el fallo del catálogo externo, no un 500")
    void previewReportsAnExternalFailure() throws Exception {
        mockMvc.perform(get("/api/books/lookup/{isbn}", "9780262033848")
                        .header(HttpHeaders.AUTHORIZATION, bearer(librarianToken())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_LOOKUP_FAILED"));
    }

    @Test
    @DisplayName("un ISBN mal formado en la previsualización devuelve 400")
    void previewRejectsAMalformedIsbn() throws Exception {
        mockMvc.perform(get("/api/books/lookup/{isbn}", "no-es-un-isbn")
                        .header(HttpHeaders.AUTHORIZATION, bearer(librarianToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ISBN"));
    }
}
