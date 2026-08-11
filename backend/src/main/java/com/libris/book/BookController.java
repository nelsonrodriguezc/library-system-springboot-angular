package com.libris.book;

import com.libris.auth.AuthenticatedUser;
import com.libris.book.dto.BookPreviewResponse;
import com.libris.book.dto.BookResponse;
import com.libris.book.dto.BookSearchQuery;
import com.libris.book.dto.CreateBookRequest;
import com.libris.book.recommendation.BookRecommendationService;
import com.libris.book.recommendation.dto.BookRecommendationResponse;
import com.libris.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalogue is readable by any signed-in account; only an ADMIN may change it.
 */
@Tag(name = "Catálogo", description = "Consulta del catálogo, alta por ISBN y recomendaciones")
@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    private final BookService bookService;
    private final BookRecommendationService recommendationService;

    public BookController(BookService bookService, BookRecommendationService recommendationService) {
        this.bookService = bookService;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public PageResponse<BookResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) String subject,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookService.search(new BookSearchQuery(search, status, subject), pageable);
    }

    @GetMapping("/subjects")
    public List<String> subjects() {
        return bookService.catalogueSubjects();
    }

    /**
     * Titles similar to what the reader has borrowed before. Empty for a reader with no
     * history: there is nothing honest to suggest yet.
     */
    @GetMapping("/recommendations")
    public List<BookRecommendationResponse> recommendations(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @RequestParam(defaultValue = "3") @Min(1) @Max(12) int limit) {
        return recommendationService.recommendFor(caller, limit);
    }

    /** Preview from the external catalogue. Nothing is stored by this call. */
    @GetMapping("/lookup/{isbn}")
    public BookPreviewResponse lookup(@PathVariable String isbn) {
        return bookService.preview(isbn);
    }

    @GetMapping("/{id}")
    public BookResponse detail(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
