package com.libris.notification.schedule;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets an administrator run the daily sweeps on demand.
 *
 * <p>Not part of the required API. It exists so the scheduled behaviour can actually be
 * demonstrated: waiting until 08:00 to see a reminder land in MailHog is not a reasonable
 * way to review the feature. Sitting under {@code /api/admin} it is ADMIN-only.
 */
@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationJobsController {

    private final DueSoonReminderJob dueSoonReminderJob;
    private final OverdueNoticeJob overdueNoticeJob;

    public NotificationJobsController(DueSoonReminderJob dueSoonReminderJob, OverdueNoticeJob overdueNoticeJob) {
        this.dueSoonReminderJob = dueSoonReminderJob;
        this.overdueNoticeJob = overdueNoticeJob;
    }

    @PostMapping("/due-soon-reminders")
    public Map<String, Integer> sendDueSoonReminders() {
        return Map.of("sent", dueSoonReminderJob.sendPendingReminders());
    }

    @PostMapping("/overdue-notices")
    public Map<String, Integer> sendOverdueNotices() {
        return Map.of("sent", overdueNoticeJob.sendPendingNotices());
    }
}
