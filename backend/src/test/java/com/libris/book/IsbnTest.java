package com.libris.book;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IsbnTest {

    @ParameterizedTest
    @ValueSource(strings = {"9780132350884", "978-0-13-235088-4", "978 0 13 235088 4"})
    @DisplayName("los separadores no cambian el ISBN")
    void normalisesSeparators(String raw) {
        assertThat(Isbn.normalise(raw)).isEqualTo("9780132350884");
    }

    @Test
    @DisplayName("el dígito de control X se guarda en mayúscula")
    void upperCasesTheCheckCharacter() {
        assertThat(Isbn.normalise("043942089x")).isEqualTo("043942089X");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9780132350884", "9780321125217", "0132350882", "043942089X"})
    @DisplayName("acepta ISBN-10 e ISBN-13 válidos")
    void acceptsValidIsbns(String isbn) {
        assertThat(Isbn.isValid(isbn)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1234567890123",  // dígito de control incorrecto
            "9780132350885",  // un dígito cambiado
            "978013235088",   // demasiado corto
            "97801323508844", // demasiado largo
            "abcdefghij",     // no numérico
            ""})
    @DisplayName("rechaza ISBN mal formados o con control inválido")
    void rejectsInvalidIsbns(String isbn) {
        assertThat(Isbn.isValid(isbn)).isFalse();
    }

    @Test
    @DisplayName("null no revienta")
    void toleratesNull() {
        assertThat(Isbn.normalise(null)).isNull();
        assertThat(Isbn.isValid(null)).isFalse();
    }
}
