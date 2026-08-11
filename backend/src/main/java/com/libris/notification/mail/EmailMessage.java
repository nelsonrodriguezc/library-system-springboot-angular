package com.libris.notification.mail;

public record EmailMessage(String to, String subject, String htmlBody) {
}
