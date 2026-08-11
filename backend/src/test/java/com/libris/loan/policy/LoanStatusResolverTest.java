package com.libris.loan.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.libris.book.Book;
import com.libris.loan.Loan;
import com.libris.loan.LoanStatus;
import com.libris.testsupport.TestFixtures;
import com.libris.user.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoanStatusResolverTest {

    private final LoanStatusResolver resolver = new LoanStatusResolver(TestFixtures.defaultLoanProperties());
    private final Book book = TestFixtures.book("Clean Code", "9780132350884");
    private final AppUser borrower = TestFixtures.user("lector@libris.cl");

    @Test
    @DisplayName("un préstamo reciente está ACTIVO")
    void freshLoanIsActive() {
        Loan loan = TestFixtures.openLoan(book, borrower, 1);
        assertThat(resolver.resolve(loan, TestFixtures.TODAY)).isEqualTo(LoanStatus.ACTIVO);
    }

    @Test
    @DisplayName("dentro de la ventana de aviso pasa a POR_VENCER")
    void loanInsideTheWindowIsDueSoon() {
        Loan loan = TestFixtures.openLoan(book, borrower, 12);
        assertThat(loan.daysUntilDue(TestFixtures.TODAY)).isEqualTo(2);
        assertThat(resolver.resolve(loan, TestFixtures.TODAY)).isEqualTo(LoanStatus.POR_VENCER);
    }

    @Test
    @DisplayName("el mismo día del vencimiento sigue siendo POR_VENCER, no VENCIDO")
    void dueTodayIsNotYetOverdue() {
        Loan loan = TestFixtures.openLoan(book, borrower, 14);
        assertThat(resolver.resolve(loan, TestFixtures.TODAY)).isEqualTo(LoanStatus.POR_VENCER);
    }

    @Test
    @DisplayName("pasada la fecha límite queda VENCIDO")
    void pastDueIsOverdue() {
        Loan loan = TestFixtures.openLoan(book, borrower, 20);
        assertThat(resolver.resolve(loan, TestFixtures.TODAY)).isEqualTo(LoanStatus.VENCIDO);
    }

    @Test
    @DisplayName("una vez devuelto el estado es DEVUELTO, aunque haya sido tarde")
    void returnedWinsOverEverythingElse() {
        Loan loan = TestFixtures.returnedLoan(book, borrower, 30, 5);
        assertThat(resolver.resolve(loan, TestFixtures.TODAY)).isEqualTo(LoanStatus.DEVUELTO);
    }
}
