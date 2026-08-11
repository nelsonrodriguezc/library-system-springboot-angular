package com.libris.loan.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.libris.book.Book;
import com.libris.book.BookNotAvailableException;
import com.libris.loan.LoanRepository;
import com.libris.loan.MaxActiveLoansException;
import com.libris.testsupport.TestFixtures;
import com.libris.user.AppUser;
import com.libris.user.UserBlockedException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Each eligibility rule in isolation, which is the point of having them as separate classes. */
@ExtendWith(MockitoExtension.class)
class LoanRulesTest {

    private final Book book = TestFixtures.book("Clean Code", "9780132350884");
    private final AppUser borrower = TestFixtures.user("lector@libris.cl");

    private LoanRequest requestFor(Book book, AppUser borrower) {
        return new LoanRequest(book, borrower, TestFixtures.TODAY, TestFixtures.NOW);
    }

    @Nested
    class BookAvailability {

        private final BookAvailableRule rule = new BookAvailableRule();

        @Test
        @DisplayName("un ejemplar disponible pasa")
        void availablePasses() {
            assertThatCode(() -> rule.check(requestFor(book, borrower))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un ejemplar prestado se rechaza")
        void loanedIsRejected() {
            book.markLoaned();
            assertThatThrownBy(() -> rule.check(requestFor(book, borrower)))
                    .isInstanceOf(BookNotAvailableException.class)
                    .hasMessageContaining("Clean Code");
        }

        @Test
        @DisplayName("un ejemplar reservado pasa esta regla, porque quién puede llevárselo lo decide otra")
        void reservedIsLeftToTheWaitingListRule() {
            book.markReserved();
            assertThatCode(() -> rule.check(requestFor(book, borrower))).doesNotThrowAnyException();
        }
    }

    @Nested
    class BorrowerNotBlocked {

        private final BorrowerNotBlockedRule rule = new BorrowerNotBlockedRule();

        @Test
        @DisplayName("una cuenta sin bloqueo pasa")
        void unblockedPasses() {
            assertThatCode(() -> rule.check(requestFor(book, borrower))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("una cuenta bloqueada se rechaza y el mensaje dice hasta cuándo")
        void blockedIsRejected() {
            Instant blockedUntil = TestFixtures.NOW.plus(7, ChronoUnit.DAYS);
            borrower.blockUntil(blockedUntil);

            // El mensaje se muestra en la zona horaria de la biblioteca, así que la fecha
            // esperada se deriva igual: fijarla a mano ataría el test a una zona concreta.
            String expectedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    .format(blockedUntil.atZone(ZoneId.systemDefault()));

            assertThatThrownBy(() -> rule.check(requestFor(book, borrower)))
                    .isInstanceOf(UserBlockedException.class)
                    .hasMessageContaining("lector@libris.cl")
                    .hasMessageContaining(expectedDate);
        }

        @Test
        @DisplayName("un bloqueo ya vencido deja pedir de nuevo")
        void expiredBlockPasses() {
            borrower.blockUntil(TestFixtures.NOW.minus(1, ChronoUnit.SECONDS));
            assertThatCode(() -> rule.check(requestFor(book, borrower))).doesNotThrowAnyException();
        }
    }

    @Nested
    class MaxActiveLoans {

        @Mock
        private LoanRepository loans;

        @Test
        @DisplayName("por debajo del máximo pasa")
        void belowTheCapPasses() {
            when(loans.countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(anyString())).thenReturn(2L);
            MaxActiveLoansRule rule = new MaxActiveLoansRule(loans, TestFixtures.defaultLoanProperties());

            assertThatCode(() -> rule.check(requestFor(book, borrower))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("en el máximo se rechaza")
        void atTheCapIsRejected() {
            when(loans.countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(anyString())).thenReturn(3L);
            MaxActiveLoansRule rule = new MaxActiveLoansRule(loans, TestFixtures.defaultLoanProperties());

            assertThatThrownBy(() -> rule.check(requestFor(book, borrower)))
                    .isInstanceOf(MaxActiveLoansException.class)
                    .hasMessageContaining("3");
        }

        @Test
        @DisplayName("el máximo es configurable")
        void capIsConfigurable() {
            when(loans.countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(anyString())).thenReturn(1L);
            MaxActiveLoansRule rule = new MaxActiveLoansRule(loans,
                    new com.libris.loan.LoanProperties(14, 1, 3, 2, 3, 90, 7));

            assertThatThrownBy(() -> rule.check(requestFor(book, borrower)))
                    .isInstanceOf(MaxActiveLoansException.class);
        }
    }

    @Test
    @DisplayName("cada regla lanza su propia excepción, con un código distinto")
    void eachRuleReportsItsOwnCode() {
        book.markLoaned();
        borrower.blockUntil(TestFixtures.NOW.plus(1, ChronoUnit.DAYS));

        assertThat(catchCode(() -> new BookAvailableRule().check(requestFor(book, borrower))))
                .isEqualTo("BOOK_NOT_AVAILABLE");
        assertThat(catchCode(() -> new BorrowerNotBlockedRule().check(requestFor(book, borrower))))
                .isEqualTo("USER_BLOCKED");
    }

    private String catchCode(Runnable action) {
        try {
            action.run();
            return null;
        } catch (com.libris.shared.exception.BusinessException e) {
            return e.code();
        }
    }
}
