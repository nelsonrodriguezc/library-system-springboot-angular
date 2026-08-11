package com.libris.loan;

import com.libris.book.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A book handed to a reader. The borrower is stored by name and e-mail, as required by
 * the specification; the e-mail is what links the loan back to an {@code AppUser}.
 *
 * <p>The two "sent at" stamps make the notification jobs idempotent: a reminder or an
 * overdue notice is never sent twice for the same loan.
 */
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "borrower_name", nullable = false, length = 120)
    private String borrowerName;

    @Column(name = "borrower_email", nullable = false, length = 180)
    private String borrowerEmail;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "overdue_notice_sent_at")
    private Instant overdueNoticeSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Loan() {
        // required by JPA
    }

    public Loan(Book book, String borrowerName, String borrowerEmail, LocalDate loanDate, LocalDate dueDate) {
        this.book = book;
        this.borrowerName = borrowerName;
        this.borrowerEmail = borrowerEmail;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
    }

    @PrePersist
    void assignCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    /** Still out and past its due date. */
    public boolean isOverdueOn(LocalDate today) {
        return !isReturned() && today.isAfter(dueDate);
    }

    /** Came back after the due date: this is what counts as a strike against the account. */
    public boolean wasReturnedLate() {
        return isReturned() && returnDate.isAfter(dueDate);
    }

    /** Negative once the due date has passed. */
    public long daysUntilDue(LocalDate today) {
        return ChronoUnit.DAYS.between(today, dueDate);
    }

    /** How many days late the return was, or 0 when it was on time. */
    public long daysLate(LocalDate today) {
        LocalDate reference = returnDate != null ? returnDate : today;
        long late = ChronoUnit.DAYS.between(dueDate, reference);
        return Math.max(late, 0);
    }

    public void markReturned(LocalDate date) {
        this.returnDate = date;
    }

    public void markReminderSent(Instant moment) {
        this.reminderSentAt = moment;
    }

    public void markOverdueNoticeSent(Instant moment) {
        this.overdueNoticeSentAt = moment;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public String getBorrowerEmail() {
        return borrowerEmail;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public Instant getOverdueNoticeSentAt() {
        return overdueNoticeSentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
