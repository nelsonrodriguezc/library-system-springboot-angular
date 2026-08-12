package com.libris.book.dto;

import com.libris.book.ValidIsbn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The ISBN is the only mandatory field: a book can be registered from it alone and the
 * rest is completed from the external catalogue. Anything supplied here takes precedence
 * over what that catalogue says, so a librarian can always correct bad external data.
 *
 * <p>If neither source ends up providing a title and an author, the request is rejected
 * with {@code INCOMPLETE_BOOK_DATA}.
 */
public record CreateBookRequest(

        @Size(max = 250, message = "El título no puede superar los 250 caracteres")
        String title,

        @Size(max = 180, message = "El autor no puede superar los 180 caracteres")
        String author,

        @NotBlank(message = "El ISBN es obligatorio")
        @ValidIsbn
        String isbn,

        @Min(value = 1000, message = "El año de publicación no es válido")
        @Max(value = 2200, message = "El año de publicación no es válido")
        Integer publicationYear,

        @Size(max = 500, message = "La URL de portada no puede superar los 500 caracteres")
        String coverUrl,

        String description,

        List<String> subjects) {
}
