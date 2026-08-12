package com.libris.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param from        sender address stamped on every message
 * @param appBaseUrl  used to build the links inside the e-mails
 * @param dueSoonCron when the reminder job runs
 * @param overdueCron when the overdue notice job runs
 */
@ConfigurationProperties(prefix = "libris.mail")
public record NotificationProperties(String from, String appBaseUrl, String dueSoonCron, String overdueCron) {

    public NotificationProperties {
        from = orDefault(from, "biblioteca@libris.local");
        appBaseUrl = orDefault(appBaseUrl, "http://localhost:4200");
        dueSoonCron = orDefault(dueSoonCron, "0 0 8 * * *");
        overdueCron = orDefault(overdueCron, "0 30 8 * * *");
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
