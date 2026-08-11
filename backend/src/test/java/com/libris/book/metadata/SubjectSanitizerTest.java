package com.libris.book.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SubjectSanitizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "005.13/3",                 // Dewey
            "813.54",                   // Dewey
            "Qa76.73.j38",              // Library of Congress
            "Qa76.73.j38 b57 2001",     // Library of Congress with edition
            "QA76",                     // Library of Congress, short form
            "j.",                       // too short to mean anything
            "1-2-3"})                   // mostly punctuation and digits
    @DisplayName("descarta códigos de clasificación y ruido")
    void dropsShelvingCodes(String noise) {
        assertThat(SubjectSanitizer.clean(List.of(noise), 10)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Object-oriented programming (Computer science)",
            "Java (Computer program language)",
            "Software engineering",
            "Agile software development",
            "Java 2.",
            "Literary collections"})
    @DisplayName("conserva los temas reales, incluso si llevan un número")
    void keepsRealSubjects(String subject) {
        assertThat(SubjectSanitizer.clean(List.of(subject), 10)).containsExactly(subject);
    }

    @Test
    @DisplayName("filtra una lista real de Open Library dejando solo lo legible")
    void cleansARealResponse() {
        List<String> fromOpenLibrary = List.of(
                "Java (Computer program language)",
                "Object-oriented programming (Computer science)",
                "Java 2.",
                "Qa76.73.j38",
                "Qa76.73.j38 b57 2001",
                "005.13/3",
                "Literary collections");

        assertThat(SubjectSanitizer.clean(fromOpenLibrary, 10)).containsExactly(
                "Java (Computer program language)",
                "Object-oriented programming (Computer science)",
                "Java 2.",
                "Literary collections");
    }

    @Test
    @DisplayName("quita repetidos, recorta y tolera nulos")
    void deduplicatesTrimsAndTolueratesNulls() {
        assertThat(SubjectSanitizer.clean(Arrays.asList("  Testing  ", "Testing", null, ""), 10))
                .containsExactly("Testing");
        assertThat(SubjectSanitizer.clean(null, 10)).isEmpty();
    }

    @Test
    @DisplayName("respeta el límite pedido")
    void honoursTheLimit() {
        assertThat(SubjectSanitizer.clean(List.of("Uno", "Dos", "Tres", "Cuatro"), 2))
                .containsExactly("Uno", "Dos");
    }
}
