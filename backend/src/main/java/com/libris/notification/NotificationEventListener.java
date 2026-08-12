package com.libris.notification;

import com.libris.config.AsyncConfig;
import com.libris.loan.event.LoanCreatedEvent;
import com.libris.notification.port.AccountNotifier;
import com.libris.notification.port.LoanNotifier;
import com.libris.notification.port.ReservationNotifier;
import com.libris.reservation.event.BookAvailableEvent;
import com.libris.user.event.AccountBlockedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns domain events into e-mails.
 *
 * <p>Two annotations carry the design. {@code AFTER_COMMIT} means a message can never
 * describe something that was rolled back, and {@code @Async} means the HTTP request that
 * created the loan returns without waiting for SMTP.
 *
 * <p>Failures are swallowed after logging on purpose: the loan is already recorded, and
 * an unreachable mail server must not turn into a lost transaction or a noisy retry loop.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final LoanNotifier loanNotifier;
    private final AccountNotifier accountNotifier;
    private final ReservationNotifier reservationNotifier;

    public NotificationEventListener(LoanNotifier loanNotifier,
                                     AccountNotifier accountNotifier,
                                     ReservationNotifier reservationNotifier) {
        this.loanNotifier = loanNotifier;
        this.accountNotifier = accountNotifier;
        this.reservationNotifier = reservationNotifier;
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanCreated(LoanCreatedEvent event) {
        safely("confirmación de préstamo " + event.loanId(),
                () -> loanNotifier.sendLoanConfirmation(event.loanId()));
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountBlocked(AccountBlockedEvent event) {
        safely("aviso de bloqueo de la cuenta " + event.userId(),
                () -> accountNotifier.sendAccountBlocked(event.userId()));
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookAvailable(BookAvailableEvent event) {
        safely("aviso de libro disponible para la reserva " + event.reservationId(),
                () -> reservationNotifier.sendBookAvailable(event.reservationId()));
    }

    private void safely(String description, Runnable delivery) {
        try {
            delivery.run();
        } catch (RuntimeException e) {
            log.error("No se pudo enviar la {}", description, e);
        }
    }
}
