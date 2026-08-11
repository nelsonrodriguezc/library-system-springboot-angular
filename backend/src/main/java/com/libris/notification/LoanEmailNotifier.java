package com.libris.notification;

import com.libris.loan.Loan;
import com.libris.loan.LoanRepository;
import com.libris.notification.mail.EmailMessage;
import com.libris.notification.mail.EmailSender;
import com.libris.notification.mail.TemplateRenderer;
import com.libris.notification.port.LoanNotifier;
import com.libris.shared.exception.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds and sends the three loan e-mails. Each one is a template plus a model; the
 * wording lives in {@code resources/templates/email} rather than in Java strings.
 */
@Component
public class LoanEmailNotifier implements LoanNotifier {

    private final LoanRepository loans;
    private final TemplateRenderer renderer;
    private final EmailSender emailSender;
    private final NotificationProperties properties;
    private final Clock clock;

    public LoanEmailNotifier(LoanRepository loans,
                             TemplateRenderer renderer,
                             EmailSender emailSender,
                             NotificationProperties properties,
                             Clock clock) {
        this.loans = loans;
        this.renderer = renderer;
        this.emailSender = emailSender;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendLoanConfirmation(Long loanId) {
        Loan loan = require(loanId);
        Map<String, Object> model = baseModel(loan);
        emailSender.send(new EmailMessage(
                loan.getBorrowerEmail(),
                "Préstamo confirmado: " + loan.getBook().getTitle(),
                renderer.render("loan-confirmation", model)));
    }

    @Override
    @Transactional(readOnly = true)
    public void sendDueSoonReminder(Long loanId) {
        Loan loan = require(loanId);
        long daysLeft = loan.daysUntilDue(LocalDate.now(clock));
        Map<String, Object> model = baseModel(loan);
        model.put("daysLeft", daysLeft);

        String subject = daysLeft == 1
                ? "Tu préstamo vence mañana: " + loan.getBook().getTitle()
                : "Tu préstamo vence en %d días: %s".formatted(daysLeft, loan.getBook().getTitle());

        emailSender.send(new EmailMessage(loan.getBorrowerEmail(), subject,
                renderer.render("due-soon-reminder", model)));
    }

    @Override
    @Transactional(readOnly = true)
    public void sendOverdueNotice(Long loanId) {
        Loan loan = require(loanId);
        Map<String, Object> model = baseModel(loan);
        model.put("daysLate", loan.daysLate(LocalDate.now(clock)));

        emailSender.send(new EmailMessage(
                loan.getBorrowerEmail(),
                "Préstamo vencido: " + loan.getBook().getTitle(),
                renderer.render("overdue-notice", model)));
    }

    private Map<String, Object> baseModel(Loan loan) {
        Map<String, Object> model = new HashMap<>();
        model.put("borrowerName", loan.getBorrowerName());
        model.put("bookTitle", loan.getBook().getTitle());
        model.put("bookAuthor", loan.getBook().getAuthor());
        model.put("coverUrl", loan.getBook().getCoverUrl());
        model.put("loanDate", SpanishDates.format(loan.getLoanDate()));
        model.put("dueDate", SpanishDates.format(loan.getDueDate()));
        model.put("appUrl", properties.appBaseUrl());
        return model;
    }

    private Loan require(Long loanId) {
        return loans.findById(loanId).orElseThrow(() -> new NotFoundException("Préstamo", loanId));
    }
}
