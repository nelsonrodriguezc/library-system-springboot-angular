package com.libris.notification.port;

/**
 * Messages that concern a specific loan.
 *
 * <p>Split from the other notifier ports on purpose: the scheduled jobs only ever need
 * this one, and depending on a single five-method notification service would have tied
 * them to messages they have nothing to do with.
 */
public interface LoanNotifier {

    void sendLoanConfirmation(Long loanId);

    void sendDueSoonReminder(Long loanId);

    void sendOverdueNotice(Long loanId);
}
