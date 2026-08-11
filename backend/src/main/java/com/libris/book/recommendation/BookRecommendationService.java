package com.libris.book.recommendation;

import com.libris.auth.AuthenticatedUser;
import com.libris.book.Book;
import com.libris.book.BookRepository;
import com.libris.book.BookStatus;
import com.libris.book.dto.BookResponse;
import com.libris.book.recommendation.SubjectSimilarityScorer.WeightedSubjects;
import com.libris.book.recommendation.dto.BookRecommendationResponse;
import com.libris.loan.Loan;
import com.libris.loan.LoanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suggests titles from what the reader has borrowed before.
 *
 * <p>Cold start is handled by returning nothing rather than by padding the list with
 * popular books: a reader with no history has no profile, and the interface simply hides
 * the panel instead of pretending the suggestion means something.
 */
@Service
public class BookRecommendationService {

    /** A loan counts half as much once it is this old. */
    private static final double RECENCY_HALF_LIFE_DAYS = 180.0;
    private static final int MAX_REASONS = 3;
    private static final int HISTORY_SIZE = 50;

    private final LoanRepository loans;
    private final BookRepository books;
    private final SubjectSimilarityScorer scorer;
    private final Clock clock;

    public BookRecommendationService(LoanRepository loans,
                                     BookRepository books,
                                     SubjectSimilarityScorer scorer,
                                     Clock clock) {
        this.loans = loans;
        this.books = books;
        this.scorer = scorer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BookRecommendationResponse> recommendFor(AuthenticatedUser reader, int limit) {
        List<Loan> history = loans
                .findByBorrowerEmailIgnoreCaseOrderByLoanDateDescIdDesc(reader.email(), Pageable.ofSize(HISTORY_SIZE))
                .getContent();
        if (history.isEmpty()) {
            return List.of();
        }

        List<Book> catalogue = books.findAllWithSubjects();
        Map<String, Double> idf = scorer.inverseDocumentFrequency(documentFrequency(catalogue), catalogue.size());

        LocalDate today = LocalDate.now(clock);
        Map<String, Double> profile = scorer.buildProfile(
                history.stream()
                        .map(loan -> new WeightedSubjects(loan.getBook().getSubjects(), recencyWeight(loan, today)))
                        .toList(),
                idf);
        if (profile.isEmpty()) {
            return List.of();
        }

        Set<Long> alreadyRead = history.stream().map(loan -> loan.getBook().getId()).collect(Collectors.toSet());

        return catalogue.stream()
                .filter(book -> book.getStatus() == BookStatus.DISPONIBLE)
                .filter(book -> !alreadyRead.contains(book.getId()))
                .map(book -> new BookRecommendationResponse(
                        BookResponse.from(book),
                        scorer.similarity(profile, book.getSubjects(), idf),
                        scorer.sharedSubjects(profile, book.getSubjects(), MAX_REASONS)))
                .filter(recommendation -> recommendation.score() > 0)
                .sorted(Comparator.comparingDouble(BookRecommendationResponse::score).reversed())
                .limit(limit)
                .toList();
    }

    private Map<String, Long> documentFrequency(List<Book> catalogue) {
        return catalogue.stream()
                .flatMap(book -> book.getSubjects().stream())
                .collect(Collectors.groupingBy(subject -> subject, Collectors.counting()));
    }

    /** Exponential decay, so old reading habits fade instead of dropping off a cliff. */
    private double recencyWeight(Loan loan, LocalDate today) {
        long ageInDays = Math.max(ChronoUnit.DAYS.between(loan.getLoanDate(), today), 0);
        return Math.pow(0.5, ageInDays / RECENCY_HALF_LIFE_DAYS);
    }
}
