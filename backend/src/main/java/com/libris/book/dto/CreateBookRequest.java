package com.libris.book.dto;

import com.libris.book.ValidIsbn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Only the ISBN is strictly required to be well formed. Title and author are mandatory
 * too, but the client is expected to have filled them either by hand or with the
 * Open Library preview, which is why they are plain strings here.
 */
public record CreateBookRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 250, message = "El título no puede superar los 250 caracteres")
        String title,

        @NotBlank(message = "El autor es obligatorio")
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
