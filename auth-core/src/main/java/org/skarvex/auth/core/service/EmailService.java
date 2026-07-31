package org.skarvex.auth.core.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.skarvex.auth.core.model.EmailBindResult;
import org.skarvex.auth.core.model.EmailStatus;
import org.skarvex.auth.core.model.EmailTemplate;
import org.skarvex.auth.core.model.SmtpConfig;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EmailService {

    private final SmtpConfig config;
    private final Session session;

    private final Map<UUID, EmailStatus> pendingEmails = new ConcurrentHashMap<>();

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

    public EmailBindResult sendConfirmationCode(UUID uuid, String code, EmailTemplate template) {
        EmailStatus status = pendingEmails.getOrDefault(uuid, EmailStatus.IDLE);

        if (status == EmailStatus.AWAITING_CONFIRMATION) {
            return EmailBindResult.AWAITING_CONFIRMATION;
        }

        try {
            pendingEmails.put(uuid, EmailStatus.AWAITING_CONFIRMATION);
            sendEmail(template, code);
            return EmailBindResult.SUCCESS;
        } catch (Exception e) {
            pendingEmails.remove(uuid);
            throw e;
        }
    }

    public void confirmEmail(UUID uuid) {
        pendingEmails.remove(uuid);
    }

    public void clearPendingStatus(UUID uuid) {
        pendingEmails.remove(uuid);
    }
}
