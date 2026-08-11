package com.libris.notification.mail;

/** Delivery failed. Never reaches a client: notifications are sent off the request thread. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String recipient, Throwable cause) {
        super("No se pudo enviar el correo a " + recipient, cause);
    }
}
