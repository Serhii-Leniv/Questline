package com.questline.common;

/**
 * Outbound notification seam. The rest of the app depends only on this interface, so the channel
 * (logging now; SMTP / a provider API later) can be swapped via config without touching callers.
 */
public interface Notifier {

    void send(String toEmail, String subject, String body);
}
