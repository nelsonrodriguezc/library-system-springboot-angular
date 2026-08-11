package com.libris.notification.schedule;

import com.libris.loan.Loan;
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
 * Daily sweep for loans that are already late. Exists because the loan record carries an
 * {@code overdueNoticeSentAt} stamp, which only makes sense if something sends that
 * notice and records it.
 */
@Component
public class OverdueNoticeJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueNoticeJob.class);

    private final LoanRepository loans;
    private final LoanNotifier loanNotifier;
    private final Clock clock;

    public OverdueNoticeJob(LoanRepository loans, LoanNotifier loanNotifier, Clock clock) {
        this.loans = loans;
        this.loanNotifier = loanNotifier;
        this.clock = clock;
    }

    @Scheduled(cron = "${libris.mail.overdue-cron:0 30 8 * * *}")
    public void run() {
        int sent = sendPendingNotices();
        log.info("Overdue notice sweep sent {} messages", sent);
    }

    @Transactional
    public int sendPendingNotices() {
        List<Loan> overdue = loans.findByReturnDateIsNullAndOverdueNoticeSentAtIsNullAndDueDateBefore(
                LocalDate.now(clock));
        int sent = 0;
        for (Loan loan : overdue) {
            try {
                loanNotifier.sendOverdueNotice(loan.getId());
                loan.markOverdueNoticeSent(clock.instant());
                sent++;
            } catch (RuntimeException e) {
                log.error("Could not send the overdue notice for loan {}", loan.getId(), e);
            }
        }
        return sent;
    }
}
