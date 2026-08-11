package com.libris.book;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    long countByStatus(BookStatus status);

    @EntityGraph(attributePaths = "subjects")
    Optional<Book> findWithSubjectsById(Long id);

    /**
     * Whole catalogue with subjects loaded, used to build the recommendation vectors.
     * One query instead of one per book.
     */
    @EntityGraph(attributePaths = "subjects")
    @Query("select b from Book b")
    List<Book> findAllWithSubjects();

    /** Distinct subjects across the catalogue, used to populate the subject filter. */
    @Query("select distinct s from Book b join b.subjects s order by s")
    List<String> findDistinctSubjects();
}
