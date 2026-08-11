package com.libris.book;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A title in the catalogue. Cover and subjects are optional because they are only
 * present when the record could be enriched from Open Library.
 */
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, length = 180)
    private String author;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(columnDefinition = "text")
    private String description;

    /** Open Library subjects. They also feed the subject filter and the recommendations. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "book_subject", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "subject", length = 120)
    private Set<String> subjects = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Book() {
        // required by JPA
    }

    public Book(String title, String author, String isbn, Integer publicationYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.status = BookStatus.DISPONIBLE;
    }

    @PrePersist
    void assignCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isAvailable() {
        return status == BookStatus.DISPONIBLE;
    }

    public boolean isReserved() {
        return status == BookStatus.RESERVADO;
    }

    public void markLoaned() {
        this.status = BookStatus.PRESTADO;
    }

    public void markAvailable() {
        this.status = BookStatus.DISPONIBLE;
    }

    /** Held for the reader that was first in the waiting list. */
    public void markReserved() {
        this.status = BookStatus.RESERVADO;
    }

    public void describe(String description) {
        this.description = description;
    }

    public void assignCover(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void replaceSubjects(Collection<String> newSubjects) {
        this.subjects.clear();
        if (newSubjects != null) {
            this.subjects.addAll(newSubjects);
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public BookStatus getStatus() {
        return status;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getSubjects() {
        return Set.copyOf(subjects);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
