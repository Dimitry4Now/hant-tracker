package com.hanttracker.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailerTests {

    private static ObjectProvider<JavaMailSender> smtp() {
        return new StaticListableBeanFactory(Map.of("sender", new JavaMailSenderImpl()))
                .getBeanProvider(JavaMailSender.class);
    }

    private static ObjectProvider<JavaMailSender> noSmtp() {
        return new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);
    }

    @Test
    void aSenderWithoutAnAddressTurnsEmailOff() {
        // What an unquoted "Hant Tracker <hant@example.com>" became on the server.
        assertThat(new Mailer(smtp(), "Hant Tracker", "", false).enabled()).isFalse();
        assertThat(new Mailer(smtp(), "", "", false).enabled()).isFalse();
    }

    @Test
    void anAddressWithOrWithoutANameTurnsEmailOn() {
        assertThat(new Mailer(smtp(), "hant@example.com", "", false).enabled()).isTrue();
        assertThat(new Mailer(smtp(), "Hant Tracker <hant@example.com>", "", false).enabled()).isTrue();
        // The SMTP login is the sender when MAIL_FROM is empty.
        assertThat(new Mailer(smtp(), "", "login@example.com", false).enabled()).isTrue();
    }

    @Test
    void withoutSmtpOnlyDevsLogCounts() {
        assertThat(new Mailer(noSmtp(), "hant@example.com", "", false).enabled()).isFalse();
        assertThat(new Mailer(noSmtp(), "", "", true).enabled()).isTrue();
    }
}
