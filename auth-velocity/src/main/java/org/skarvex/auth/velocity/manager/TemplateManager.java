package org.skarvex.auth.velocity.manager;

import org.skarvex.auth.core.model.EmailTemplate;

import java.util.HashMap;
import java.util.Map;

public class TemplateManager {

    private final Map<String, EmailTemplate> templates = new HashMap<>();

    public TemplateManager(ConfigurationManager config) {
        loadTemplates(config);
    }

    public void loadTemplates(ConfigurationManager config) {
        templates.clear();

        loadTemplate(config, "binding");
        loadTemplate(config, "recovery");
    }

    private void loadTemplate(ConfigurationManager config, String key) {
        String path = "email-templates." + key;

        EmailTemplate template = new EmailTemplate(
                config.getString(path + ".from"),
                null,
                config.getString(path + ".subject"),
                config.getString(path + ".text")
        );

        templates.put(key, template);
    }

    public EmailTemplate getTemplate(String key, String recipient) {
        EmailTemplate template = templates.get(key);
        if (template == null) {
            throw new IllegalArgumentException("Email template not found: " + key);
        }
        return template.withRecipient(recipient);
    }
}
