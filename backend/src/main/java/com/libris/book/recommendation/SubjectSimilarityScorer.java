package com.libris.book.recommendation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Content-based scoring over the subjects a book carries.
 *
 * <p>This is the classic recommender baseline, and it is deliberately not a call to a
 * language model: the signal needed here is already in the catalogue, so the feature costs
 * no external service, no credentials and no latency.
 *
 * <p>Subjects are weighted by inverse document frequency, so "Software engineering" —
 * which nearly every book in a technical library carries — counts for far less than
 * "Distributed systems". Similarity is the cosine between the reader's profile vector and
 * the book's, which normalises away the fact that some books list many more subjects than
 * others.
 */
@Component
public class SubjectSimilarityScorer {

    /**
     * @param documentFrequency how many books carry each subject
     * @param totalBooks        size of the catalogue
     */
    public Map<String, Double> inverseDocumentFrequency(Map<String, Long> documentFrequency, long totalBooks) {
        Map<String, Double> idf = new HashMap<>();
        documentFrequency.forEach((subject, frequency) ->
                // +1 keeps a subject present in every book from collapsing to exactly zero.
                idf.put(subject, Math.log((double) (totalBooks + 1) / (frequency + 1)) + 1.0));
        return idf;
    }

    /**
     * Builds the reader's taste vector. Each past loan contributes its subjects weighted
     * by how recent it is, so what somebody read last month says more than what they read
     * a year ago.
     */
    public Map<String, Double> buildProfile(Collection<WeightedSubjects> history, Map<String, Double> idf) {
        Map<String, Double> profile = new HashMap<>();
        for (WeightedSubjects entry : history) {
            for (String subject : entry.subjects()) {
                double weight = entry.weight() * idf.getOrDefault(subject, 1.0);
                profile.merge(subject, weight, Double::sum);
            }
        }
        return normalise(profile);
    }

    /** Cosine similarity between the profile and a book, in the shared subject space. */
    public double similarity(Map<String, Double> profile, Set<String> bookSubjects, Map<String, Double> idf) {
        if (profile.isEmpty() || bookSubjects.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> bookVector = normalise(bookSubjects.stream()
                .collect(HashMap::new, (map, subject) -> map.put(subject, idf.getOrDefault(subject, 1.0)), HashMap::putAll));

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> component : bookVector.entrySet()) {
            Double profileWeight = profile.get(component.getKey());
            if (profileWeight != null) {
                dotProduct += profileWeight * component.getValue();
            }
        }
        return dotProduct;
    }

    /** Subjects the book and the reader's profile have in common, strongest first. */
    public List<String> sharedSubjects(Map<String, Double> profile, Set<String> bookSubjects, int limit) {
        return bookSubjects.stream()
                .filter(profile::containsKey)
                .sorted((left, right) -> Double.compare(profile.get(right), profile.get(left)))
                .limit(limit)
                .toList();
    }

    /** Scales a vector to unit length, which is what makes the dot product a cosine. */
    private Map<String, Double> normalise(Map<String, Double> vector) {
        double magnitude = Math.sqrt(vector.values().stream().mapToDouble(value -> value * value).sum());
        if (magnitude == 0.0) {
            return vector;
        }
        Map<String, Double> normalised = new HashMap<>(vector.size());
        vector.forEach((key, value) -> normalised.put(key, value / magnitude));
        return normalised;
    }

    /** One past loan: the subjects it contributed and how much its age lets it count. */
    public record WeightedSubjects(Set<String> subjects, double weight) {
    }
}
