package com.questline.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default {@link Notifier}: logs the message instead of sending it. Lets reminders work with no
 * mail server configured. A real {@code SmtpNotifier} can replace it by being the primary bean
 * (this one steps aside via {@link ConditionalOnMissingBean}).
 */
@Component
@ConditionalOnMissingBean(name = "smtpNotifier")
public class LoggingNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("[notification] to={} | {} — {}", toEmail, subject, body);
    }
}
