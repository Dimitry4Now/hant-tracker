package com.hanttracker.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sends the app's few emails: confirmation codes and reset links. It is on
 * when SPRING_MAIL_HOST points at an SMTP server; without one, everything
 * that needs email is off and the rest works (admins reset passwords). Dev
 * writes the emails to the log instead.
 */
@Service
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final JavaMailSender sender;
    private final String from;
    private final boolean logOnly;

    public Mailer(
            ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.from:}") String from,
            @Value("${spring.mail.username:}") String username,
            @Value("${app.mail.log-only:false}") boolean logOnly) {
        this.sender = sender.getIfAvailable();
        this.from = StringUtils.hasText(from) ? from : username;
        this.logOnly = logOnly;
        // Says at start whether email reached the app, like the sheet reader does.
        if (this.sender != null) {
            log.info("Email is on, sent as {}", this.from);
        } else if (logOnly) {
            log.info("Email goes to the log (app.mail.log-only)");
        } else {
            log.warn("Email is off: SPRING_MAIL_HOST is not set, so emails aren't confirmed or sent");
        }
    }

    public boolean enabled() {
        return sender != null || logOnly;
    }

    /** Sends a plain-text email; 503 when email is off or the server refuses it. */
    public void send(String to, String subject, String text) {
        if (sender == null) {
            if (!logOnly) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email is not set up");
            }
            log.info("Email to {}: {}\n{}", to, subject, text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (MailException e) {
            log.error("Could not send an email to {}", to, e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Could not send the email. Try again later.", e);
        }
    }
}
