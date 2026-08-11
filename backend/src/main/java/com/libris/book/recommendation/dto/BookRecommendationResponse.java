package com.libris.book.recommendation.dto;

import com.libris.book.dto.BookResponse;
import java.util.List;

/**
 * @param matchedSubjects why this book was suggested, so the interface can show the
 *                        reason instead of asking the reader to trust a black box.
 */
public record BookRecommendationResponse(BookResponse book, double score, List<String> matchedSubjects) {
}
