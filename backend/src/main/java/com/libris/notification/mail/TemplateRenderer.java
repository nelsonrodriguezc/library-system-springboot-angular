package com.libris.notification.mail;

import java.util.Map;

/**
 * Turns a template and a model into the HTML body of a message. Abstracted so the
 * notifiers never touch a templating engine, and so a test can assert on the model
 * instead of on rendered markup.
 */
public interface TemplateRenderer {

    String render(String templateName, Map<String, Object> model);
}
