package com.libris.notification.schedule;

import com.libris.loan.Loan;
import com.libris.loan.LoanProperties;
import com.libris.loan.LoanRepository;
import com.libris.notification.port.LoanNotifier;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily sweep for loans about to fall due.
 *
 * <p>{@code reminderSentAt} is stamped as each message goes out, so a second run on the
 * same day is a no-op and a restart cannot produce duplicates.
 */
@Component
public class DueSoonReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DueSoonReminderJob.class);

    private final LoanRepository loans;
    private final LoanNotifier loanNotifier;
    private final LoanProperties loanProperties;
    private final Clock clock;

    public DueSoonReminderJob(LoanRepository loans,
                              LoanNotifier loanNotifier,
                              LoanProperties loanProperties,
                              Clock clock) {
        this.loans = loans;
        this.loanNotifier = loanNotifier;
        this.loanProperties = loanProperties;
        this.clock = clock;
    }

    @Scheduled(cron = "${libris.mail.due-soon-cron:0 0 8 * * *}")
    public void run() {
        int sent = sendPendingReminders();
        log.info("Due-soon reminder sweep sent {} messages", sent);
    }

    /**
     * @return how many reminders were actually sent, which is what the manual trigger
     *         reports back to an administrator.
     */
    @Transactional
    public int sendPendingReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalDate horizon = today.plusDays(loanProperties.reminderDaysBefore());

        List<Loan> dueSoon = loans.findByReturnDateIsNullAndReminderSentAtIsNullAndDueDateBetween(today, horizon);
        int sent = 0;
        for (Loan loan : dueSoon) {
            try {
                loanNotifier.sendDueSoonReminder(loan.getId());
                loan.markReminderSent(clock.instant());
                sent++;
            } catch (RuntimeException e) {
                // One unreachable address must not stop the rest of the sweep. The stamp
                // is left untouched so the next run tries this loan again.
                log.error("Could not remind loan {}", loan.getId(), e);
            }
        }
        return sent;
    }
}
