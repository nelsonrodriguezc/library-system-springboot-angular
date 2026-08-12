package com.libris.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.libris.book.Book;
import com.libris.loan.Loan;
import com.libris.loan.LoanRepository;
import com.libris.testsupport.TestFixtures;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The rule that matters most: three late returns in ninety days costs the account its rights. */
@ExtendWith(MockitoExtension.class)
class OverdueBlockPolicyTest {

    @Mock
    private LoanRepository loans;

    private OverdueBlockPolicy policy;
    private AppUser borrower;
    private Book book;

    @BeforeEach
    void setUp() {
        policy = new OverdueBlockPolicy(loans, TestFixtures.defaultLoanProperties(), TestFixtures.fixedClock());
        borrower = TestFixtures.user("lector@libris.cl");
        book = TestFixtures.book("Refactoring", "9780201485677");
    }

    @Test
    @DisplayName("una devolución a tiempo no cuenta como atraso")
    void onTimeReturnIsNotAStrike() {
        Loan onTime = TestFixtures.returnedLoan(book, borrower, 20, -2);

        assertThat(policy.applyAfterReturn(borrower, onTime)).isFalse();
        assertThat(borrower.getBlockedUntil()).isNull();
        verify(loans, never()).countLateReturnsSince(any(), any());
    }

    @Test
    @DisplayName("el segundo atraso todavía no bloquea")
    void secondStrikeDoesNotBlock() {
        when(loans.countLateReturnsSince(eq(borrower.getEmail()), any())).thenReturn(2L);

        assertThat(policy.applyAfterReturn(borrower, TestFixtures.returnedLoan(book, borrower, 20, 3))).isFalse();
        assertThat(borrower.getBlockedUntil()).isNull();
    }

    @Test
    @DisplayName("el tercer atraso bloquea la cuenta durante una semana")
    void thirdStrikeBlocksForAWeek() {
        when(loans.countLateReturnsSince(eq(borrower.getEmail()), any())).thenReturn(3L);

        assertThat(policy.applyAfterReturn(borrower, TestFixtures.returnedLoan(book, borrower, 20, 3))).isTrue();
        assertThat(borrower.getBlockedUntil()).isEqualTo(TestFixtures.NOW.plus(7, ChronoUnit.DAYS));
        assertThat(borrower.isBlockedAt(TestFixtures.NOW)).isTrue();
    }

    @Test
    @DisplayName("solo cuentan los atrasos dentro de la ventana de 90 días")
    void countsOnlyInsideTheWindow() {
        when(loans.countLateReturnsSince(eq(borrower.getEmail()), any())).thenReturn(3L);

        policy.applyAfterReturn(borrower, TestFixtures.returnedLoan(book, borrower, 20, 3));

        verify(loans).countLateReturnsSince(borrower.getEmail(), LocalDate.of(2026, 5, 13));
    }

    @Test
    @DisplayName("una cuenta ya bloqueada no se vuelve a bloquear ni se le avisa dos veces")
    void doesNotRenotifyAnAlreadyBlockedAccount() {
        borrower.blockUntil(TestFixtures.NOW.plus(3, ChronoUnit.DAYS));

        assertThat(policy.applyAfterReturn(borrower, TestFixtures.returnedLoan(book, borrower, 20, 3))).isFalse();
        assertThat(borrower.getBlockedUntil()).isEqualTo(TestFixtures.NOW.plus(3, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("un bloqueo ya expirado no impide uno nuevo")
    void anExpiredBlockDoesNotProtectFromANewOne() {
        borrower.blockUntil(TestFixtures.NOW.minus(1, ChronoUnit.DAYS));
        when(loans.countLateReturnsSince(eq(borrower.getEmail()), any())).thenReturn(3L);

        assertThat(policy.applyAfterReturn(borrower, TestFixtures.returnedLoan(book, borrower, 20, 3))).isTrue();
        assertThat(borrower.getBlockedUntil()).isEqualTo(TestFixtures.NOW.plus(7, ChronoUnit.DAYS));
    }
}
