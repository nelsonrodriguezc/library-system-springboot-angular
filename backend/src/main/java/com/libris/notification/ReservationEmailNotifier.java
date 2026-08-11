package com.libris.notification;

import com.libris.notification.mail.EmailMessage;
import com.libris.notification.mail.EmailSender;
import com.libris.notification.mail.TemplateRenderer;
import com.libris.notification.port.ReservationNotifier;
import com.libris.reservation.Reservation;
import com.libris.reservation.ReservationRepository;
import com.libris.shared.exception.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationEmailNotifier implements ReservationNotifier {

    private final ReservationRepository reservations;
    private final TemplateRenderer renderer;
    private final EmailSender emailSender;
    private final NotificationProperties properties;

    public ReservationEmailNotifier(ReservationRepository reservations,
                                    TemplateRenderer renderer,
                                    EmailSender emailSender,
                                    NotificationProperties properties) {
        this.reservations = reservations;
        this.renderer = renderer;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendBookAvailable(Long reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva", reservationId));

        Map<String, Object> model = new HashMap<>();
        model.put("requesterName", reservation.getRequesterName());
        model.put("bookTitle", reservation.getBook().getTitle());
        model.put("bookAuthor", reservation.getBook().getAuthor());
        model.put("coverUrl", reservation.getBook().getCoverUrl());
        model.put("appUrl", properties.appBaseUrl());

        emailSender.send(new EmailMessage(
                reservation.getRequesterEmail(),
                "Ya está disponible para ti: " + reservation.getBook().getTitle(),
                renderer.render("book-available", model)));
    }
}
