package com.libris.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.libris.auth.AuthenticatedUser;
import com.libris.book.Book;
import com.libris.book.BookNotAvailableException;
import com.libris.book.BookRepository;
import com.libris.book.BookStatus;
import com.libris.loan.dto.CreateLoanRequest;
import com.libris.loan.dto.LoanResponse;
import com.libris.loan.event.LoanCreatedEvent;
import com.libris.loan.policy.DueDateCalculator;
import com.libris.loan.policy.LoanStatusResolver;
import com.libris.loan.rule.BookAvailableRule;
import com.libris.loan.rule.BorrowerNotBlockedRule;
import com.libris.loan.rule.MaxActiveLoansRule;
import com.libris.shared.exception.NotFoundException;
import com.libris.testsupport.TestFixtures;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import com.libris.user.OverdueBlockPolicy;
import com.libris.user.UserBlockedException;
import com.libris.user.UserRole;
import com.libris.user.event.AccountBlockedEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanServiceTest {

    @Mock private LoanRepository loans;
    @Mock private BookRepository books;
    @Mock private AppUserRepository users;
    @Mock private OverdueBlockPolicy overdueBlockPolicy;
    @Mock private ApplicationEventPublisher events;
    @Mock private LoanLifecycleListener lifecycleListener;

    private LoanService service;
    private Book book;
    private AppUser borrower;
    private AuthenticatedUser caller;

    @BeforeEach
    void setUp() {
        LoanProperties properties = TestFixtures.defaultLoanProperties();
        service = new LoanService(
                loans, books, users,
                List.of(new BookAvailableRule(), new BorrowerNotBlockedRule(), new MaxActiveLoansRule(loans, properties)),
                List.of(lifecycleListener),
                new DueDateCalculator(properties),
                new LoanStatusResolver(properties),
                overdueBlockPolicy,
                events,
                TestFixtures.fixedClock());

        book = TestFixtures.book("Clean Code", "9780132350884");
        borrower = TestFixtures.user("lector@libris.cl");
        caller = new AuthenticatedUser(1L, "Lectora de Prueba", "lector@libris.cl", UserRole.BIBLIOTECARIO);

        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(users.findByEmailIgnoreCase("lector@libris.cl")).thenReturn(Optional.of(borrower));
        when(loans.countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(anyString())).thenReturn(0L);
        when(loans.save(any(Loan.class))).thenAnswer(invocation ->
                TestFixtures.withId(invocation.getArgument(0), 100L));
    }

    @Test
    @DisplayName("un préstamo válido marca el libro prestado y fija la fecha límite a 14 días")
    void createsALoan() {
        LoanResponse response = service.create(new CreateLoanRequest(10L, null), caller);

        assertThat(response.loanDate()).isEqualTo(TestFixtures.TODAY);
        assertThat(response.dueDate()).isEqualTo(TestFixtures.TODAY.plusDays(14));
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVO);
        assertThat(response.borrowerEmail()).isEqualTo("lector@libris.cl");
        assertThat(book.getStatus()).isEqualTo(BookStatus.PRESTADO);
    }

    @Test
    @DisplayName("el préstamo publica el evento que dispara el correo de confirmación")
    void publishesTheLoanCreatedEvent() {
        service.create(new CreateLoanRequest(10L, null), caller);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(LoanCreatedEvent.class);
        assertThat(((LoanCreatedEvent) captor.getValue()).loanId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("se avisa a los módulos enganchados al ciclo de vida del préstamo")
    void notifiesLifecycleListeners() {
        service.create(new CreateLoanRequest(10L, null), caller);
        verify(lifecycleListener).onLoanCreated(any(Loan.class));
    }

    @Test
    @DisplayName("no se presta un ejemplar que ya está fuera")
    void rejectsAnUnavailableCopy() {
        book.markLoaned();

        assertThatThrownBy(() -> service.create(new CreateLoanRequest(10L, null), caller))
                .isInstanceOf(BookNotAvailableException.class);
        verify(loans, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("una cuenta bloqueada no puede pedir prestado")
    void rejectsABlockedBorrower() {
        borrower.blockUntil(TestFixtures.NOW.plus(2, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.create(new CreateLoanRequest(10L, null), caller))
                .isInstanceOf(UserBlockedException.class);
        verify(loans, never()).save(any());
    }

    @Test
    @DisplayName("no se superan los préstamos activos permitidos")
    void rejectsWhenTheCapIsReached() {
        when(loans.countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(anyString())).thenReturn(3L);

        assertThatThrownBy(() -> service.create(new CreateLoanRequest(10L, null), caller))
                .isInstanceOf(MaxActiveLoansException.class);
    }

    @Test
    @DisplayName("un libro inexistente da 404 antes de evaluar ninguna regla")
    void rejectsAnUnknownBook() {
        when(books.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateLoanRequest(999L, null), caller))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("quien no es ADMIN no puede prestar a nombre de otra persona")
    void nonAdminCannotBorrowForSomeoneElse() {
        assertThatThrownBy(() ->
                service.create(new CreateLoanRequest(10L, "otro@libris.cl"), caller))
                .isInstanceOf(LoanAccessDeniedException.class);
    }

    @Test
    @DisplayName("un ADMIN sí puede registrar el préstamo a nombre de otra cuenta")
    void adminCanBorrowOnBehalfOfAnother() {
        AuthenticatedUser admin = new AuthenticatedUser(2L, "Administradora", "admin@libris.cl", UserRole.ADMIN);
        when(users.findByEmailIgnoreCase("lector@libris.cl")).thenReturn(Optional.of(borrower));

        LoanResponse response = service.create(new CreateLoanRequest(10L, "lector@libris.cl"), admin);

        assertThat(response.borrowerEmail()).isEqualTo("lector@libris.cl");
        assertThat(response.borrowerName()).isEqualTo("Lectora de Prueba");
    }

    @Test
    @DisplayName("la devolución deja el libro disponible y registra la fecha")
    void returnsALoan() {
        Loan loan = TestFixtures.openLoan(book, borrower, 5);
        book.markLoaned();
        when(loans.findById(100L)).thenReturn(Optional.of(loan));

        LoanResponse response = service.returnLoan(100L, caller);

        assertThat(response.status()).isEqualTo(LoanStatus.DEVUELTO);
        assertThat(response.returnDate()).isEqualTo(TestFixtures.TODAY);
        assertThat(book.getStatus()).isEqualTo(BookStatus.DISPONIBLE);
        verify(lifecycleListener).onLoanReturned(loan);
    }

    @Test
    @DisplayName("una devolución tardía que agota los avisos publica el evento de bloqueo")
    void publishesAccountBlockedWhenThePolicySaysSo() {
        Loan loan = TestFixtures.openLoan(book, borrower, 20);
        book.markLoaned();
        when(loans.findById(100L)).thenReturn(Optional.of(loan));
        when(overdueBlockPolicy.applyAfterReturn(borrower, loan)).thenReturn(true);

        service.returnLoan(100L, caller);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AccountBlockedEvent.class);
    }

    @Test
    @DisplayName("si la política no bloquea, no se avisa a nadie")
    void doesNotPublishWhenNoBlockHappens() {
        Loan loan = TestFixtures.openLoan(book, borrower, 20);
        book.markLoaned();
        when(loans.findById(100L)).thenReturn(Optional.of(loan));
        when(overdueBlockPolicy.applyAfterReturn(borrower, loan)).thenReturn(false);

        service.returnLoan(100L, caller);

        verify(events, never()).publishEvent(any(AccountBlockedEvent.class));
    }

    @Test
    @DisplayName("un préstamo ya devuelto no se puede devolver otra vez")
    void rejectsADoubleReturn() {
        Loan loan = TestFixtures.openLoan(book, borrower, 20);
        loan.markReturned(LocalDate.of(2026, 8, 1));
        when(loans.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnLoan(100L, caller))
                .isInstanceOf(LoanAlreadyReturnedException.class);
    }

    @Test
    @DisplayName("solo el titular o un ADMIN pueden devolver un préstamo")
    void protectsSomeoneElsesLoan() {
        Loan loan = TestFixtures.openLoan(book, borrower, 5);
        when(loans.findById(100L)).thenReturn(Optional.of(loan));
        AuthenticatedUser other = new AuthenticatedUser(9L, "Otra", "otra@libris.cl", UserRole.BIBLIOTECARIO);

        assertThatThrownBy(() -> service.returnLoan(100L, other))
                .isInstanceOf(LoanAccessDeniedException.class);

        AuthenticatedUser admin = new AuthenticatedUser(2L, "Admin", "admin@libris.cl", UserRole.ADMIN);
        assertThat(service.returnLoan(100L, admin).status()).isEqualTo(LoanStatus.DEVUELTO);
    }
}
