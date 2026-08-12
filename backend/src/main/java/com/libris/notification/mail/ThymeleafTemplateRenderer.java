package com.libris.notification.mail;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    private static final String TEMPLATE_FOLDER = "email/";
    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private final TemplateEngine templateEngine;

    public ThymeleafTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String render(String templateName, Map<String, Object> model) {
        Context context = new Context(SPANISH);
        context.setVariables(model);
        return templateEngine.process(TEMPLATE_FOLDER + templateName, context);
    }
}
