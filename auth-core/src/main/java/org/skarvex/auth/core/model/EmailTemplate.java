package org.skarvex.auth.core.model;

public record EmailTemplate(String from,
                            String recipient,
                            String subject,
                            String text) {

    public EmailTemplate withRecipient(String recipient) {
        return new EmailTemplate(this.from, recipient, this.subject, this.text);
    }
}
