package com.libris.notification.mail;

import com.libris.notification.NotificationProperties;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SpringEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SpringEmailSender.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SpringEmailSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            mailSender.send(mimeMessage);
            log.info("Sent \"{}\" to {}", message.subject(), message.to());
        } catch (MailException | jakarta.mail.MessagingException e) {
            // Surfaced to the caller, which decides whether the delivery is worth retrying.
            throw new EmailDeliveryException(message.to(), e);
        }
    }
}
