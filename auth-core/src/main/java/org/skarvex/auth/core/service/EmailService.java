package org.skarvex.auth.core.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.skarvex.auth.core.model.EmailTemplate;
import org.skarvex.auth.core.model.SmtpConfig;
import org.skarvex.auth.core.result.EmailBindResult;

import java.util.Properties;

public class EmailService {

    private final SmtpConfig config;
    private final Session session;

    public EmailService(SmtpConfig config) {
        this.config = config;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.host());
        props.put("mail.smtp.port", String.valueOf(config.port()));

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username(), config.password());
            }
        });
    }

    public void sendEmail(EmailTemplate template, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(template.from()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(template.recipient()));
            message.setSubject(template.subject());

            String text = (code != null) ? template.text().replace("<code>", code) : template.text();
            message.setText(text);

            Transport.send(message);
        } catch (MessagingException exception) {
            throw new RuntimeException("Failed to send email to " + template.recipient(), exception);
        }
    }

    public void sendEmail(EmailTemplate template) {
        sendEmail(template, null);
    }

    public EmailBindResult sendConfirmationCode(String code, EmailTemplate template) {
        sendEmail(template, code);
        return EmailBindResult.SUCCESS;
    }
}
