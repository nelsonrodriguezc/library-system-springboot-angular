package com.libris.notification.mail;

/**
 * Port for getting a message out of the building. Keeping this abstract is what lets the
 * notification logic be tested without an SMTP server, and what would let the library
 * move to a transactional e-mail API without touching a single notifier.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
