package com.libris.book.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.libris.book.recommendation.SubjectSimilarityScorer.WeightedSubjects;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubjectSimilarityScorerTest {

    private final SubjectSimilarityScorer scorer = new SubjectSimilarityScorer();

    private Map<String, Double> idfOverCatalogue() {
        // "Software engineering" aparece en casi todos los libros, "Algorithms" en pocos.
        return scorer.inverseDocumentFrequency(
                Map.of("Software engineering", 18L, "Algorithms", 2L, "Testing", 4L), 20);
    }

    @Test
    @DisplayName("un tema poco frecuente pesa más que uno omnipresente")
    void rareSubjectsWeighMore() {
        Map<String, Double> idf = idfOverCatalogue();
        assertThat(idf.get("Algorithms")).isGreaterThan(idf.get("Testing"));
        assertThat(idf.get("Testing")).isGreaterThan(idf.get("Software engineering"));
    }

    @Test
    @DisplayName("un tema que no está en el catálogo no rompe el cálculo")
    void unknownSubjectsFallBackToNeutralWeight() {
        assertThat(scorer.similarity(Map.of("Desconocido", 1.0), Set.of("Desconocido"), Map.of()))
                .isGreaterThan(0.0);
    }

    @Test
    @DisplayName("sin historial no hay perfil y la similitud es cero")
    void anEmptyProfileScoresZero() {
        assertThat(scorer.similarity(Map.of(), Set.of("Algorithms"), idfOverCatalogue())).isZero();
    }

    @Test
    @DisplayName("un libro sin temas no se puede comparar")
    void aBookWithoutSubjectsScoresZero() {
        Map<String, Double> profile = scorer.buildProfile(
                List.of(new WeightedSubjects(Set.of("Algorithms"), 1.0)), idfOverCatalogue());
        assertThat(scorer.similarity(profile, Set.of(), idfOverCatalogue())).isZero();
    }

    @Test
    @DisplayName("un libro que comparte el tema raro puntúa más que uno que comparte el común")
    void sharingARareSubjectBeatsSharingACommonOne() {
        Map<String, Double> idf = idfOverCatalogue();
        Map<String, Double> profile = scorer.buildProfile(
                List.of(new WeightedSubjects(Set.of("Algorithms", "Software engineering"), 1.0)), idf);

        double rareMatch = scorer.similarity(profile, Set.of("Algorithms"), idf);
        double commonMatch = scorer.similarity(profile, Set.of("Software engineering"), idf);

        assertThat(rareMatch).isGreaterThan(commonMatch);
    }

    @Test
    @DisplayName("un préstamo reciente influye más que uno antiguo")
    void recentHistoryWeighsMore() {
        Map<String, Double> idf = idfOverCatalogue();
        Map<String, Double> profile = scorer.buildProfile(List.of(
                new WeightedSubjects(Set.of("Algorithms"), 1.0),
                new WeightedSubjects(Set.of("Testing"), 0.1)), idf);

        assertThat(scorer.similarity(profile, Set.of("Algorithms"), idf))
                .isGreaterThan(scorer.similarity(profile, Set.of("Testing"), idf));
    }

    @Test
    @DisplayName("un libro con muchos temas no gana solo por tener más")
    void normalisationPreventsLongSubjectListsFromWinning() {
        Map<String, Double> idf = scorer.inverseDocumentFrequency(
                Map.of("A", 2L, "B", 2L, "C", 2L, "D", 2L), 20);
        Map<String, Double> profile = scorer.buildProfile(
                List.of(new WeightedSubjects(Set.of("A"), 1.0)), idf);

        double focused = scorer.similarity(profile, Set.of("A"), idf);
        double padded = scorer.similarity(profile, Set.of("A", "B", "C", "D"), idf);

        assertThat(focused).isGreaterThan(padded);
    }

    @Test
    @DisplayName("la explicación son los temas compartidos, del más relevante al menos")
    void explainsWithTheSharedSubjects() {
        Map<String, Double> idf = idfOverCatalogue();
        Map<String, Double> profile = scorer.buildProfile(
                List.of(new WeightedSubjects(Set.of("Algorithms", "Software engineering"), 1.0)), idf);

        assertThat(scorer.sharedSubjects(profile, Set.of("Software engineering", "Algorithms", "Testing"), 3))
                .containsExactly("Algorithms", "Software engineering");
    }
}
