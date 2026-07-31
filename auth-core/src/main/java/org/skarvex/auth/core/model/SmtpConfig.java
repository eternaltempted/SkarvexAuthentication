package org.skarvex.auth.core.model;

public record SmtpConfig(
        String host,
        int port,
        String username,
        String password
) {
}
