package com.libris.notification;

import com.libris.loan.LoanProperties;
import com.libris.notification.mail.EmailMessage;
import com.libris.notification.mail.EmailSender;
import com.libris.notification.mail.TemplateRenderer;
import com.libris.notification.port.AccountNotifier;
import com.libris.shared.exception.NotFoundException;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountEmailNotifier implements AccountNotifier {

    private final AppUserRepository users;
    private final TemplateRenderer renderer;
    private final EmailSender emailSender;
    private final NotificationProperties properties;
    private final LoanProperties loanProperties;

    public AccountEmailNotifier(AppUserRepository users,
                                TemplateRenderer renderer,
                                EmailSender emailSender,
                                NotificationProperties properties,
                                LoanProperties loanProperties) {
        this.users = users;
        this.renderer = renderer;
        this.emailSender = emailSender;
        this.properties = properties;
        this.loanProperties = loanProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendAccountBlocked(Long userId) {
        AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("Cuenta", userId));

        Map<String, Object> model = new HashMap<>();
        model.put("userName", user.getName());
        model.put("blockedUntil", SpanishDates.format(user.getBlockedUntil()));
        model.put("overdueLimit", loanProperties.overdueLimit());
        model.put("windowDays", loanProperties.overdueWindowDays());
        model.put("appUrl", properties.appBaseUrl());

        emailSender.send(new EmailMessage(
                user.getEmail(),
                "Tu cuenta quedó bloqueada temporalmente para pedir préstamos",
                renderer.render("account-blocked", model)));
    }
}
